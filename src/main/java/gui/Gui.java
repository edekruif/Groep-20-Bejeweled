package gui;

import board.Coordinate;
import game.Game;
import jewel.Colour;
import jewel.Jewel;
import observers.BoardObserver;
import xmlparser.XmlParser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class handles the graphical representation of our Bejeweled game.
 * 
 * @author Group 20
 */
public class Gui extends JFrame implements ActionListener, BoardObserver {
	
	/**
	 * Default serialization ID.
	 */
	private static final long serialVersionUID = 1L;
	private GridBagLayout gbl = new GridBagLayout();
	private JPanel pane = new JPanel(gbl);
	private JButton[][] allButtons = new JButton[8][8];
	private JButton newGameButton = new JButton("New Game");
	private BackgroundPanel bgPanel;
	private Image bgImage;
	private static XmlParser xmlParser = new XmlParser();
	
	private Game game;
	private static Gui gui;
	private static ImgLoader imgloader;
	public static SoundLoader soundloader;
	public static String saveGamePath = (System.getProperty("user.dir")
			+ File.separator + "savegame/Autosave.xml");

	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	/**
	 * Main method used to verify what the GUI looks like.
	 * 
	 * @param args
	 *     Input Array of Strings (for cmd line).
	 * @throws UnsupportedAudioFileException
	 *     If type of audio file is not supported.
	 * @throws LineUnavailableException
	 *     If an audio line cannot be opened because it is unavailable.
	 */
	public static void main(String[] args) throws IOException, LineUnavailableException,
		UnsupportedAudioFileException {
		Game game = loadGame();
		gui = new Gui(game);
		gui.setVisible(true);
		game.startTimer();
	}
	
	/**
	 * GUI Constructor method.
	 * 
	 * @param board
	 *     Board Object the GUI will make a graphical interface for.
	 */
	public Gui(Game game) throws IOException, LineUnavailableException,
		UnsupportedAudioFileException {
		this.game = game;
		this.addWindowListener(new Autosaver(this.game, saveGamePath));
		imgloader = new ImgLoader();
		soundloader = new SoundLoader();
		setSize(800,800);
		setResizable(false);
		bgPanel = new BackgroundPanel(getBackgroundImage());
		add(bgPanel, BorderLayout.CENTER);
		createButtons();
		createGridPane();
		StatsPanel sc = new StatsPanel(game.getLevel(), game.getScore(),
				game.getTimeLeft(), game.goalScore());
		sc.levelChanged(game.getLevel());
		sc.scoreChanged(game.getScore());
		bgPanel.add(sc, BorderLayout.NORTH);
		this.game.getBoard().addBoardObserver(this);
		this.game.addStatsObserver(sc);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	/**
	 * Gets the background image for the GUI.
	 * 
	 * @return Image
	 *     The background image.
	 */
	private Image getBackgroundImage() {
		try {
			bgImage = ImageIO.read(new File((System.getProperty("user.dir")
				+ File.separator + "src" + File.separator + "main" + File.separator
				+ "java" + File.separator + "background.jpg")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bgImage;
	}
	
	/**
	 * Creates the pane with the button grid for the GUI.
	 */
	private void createGridPane() {
		pane.setLayout(gbl);
    	GridBagConstraints constraint = new GridBagConstraints();
    	constraint.fill = GridBagConstraints.BOTH;
    	constraint.insets = new Insets(5,5,5,5);
		
		for (int y = 0; y < 8; y++) {
			constraint.gridy = y;
			for (int x = 0; x < 8; x++) {
				constraint.gridx = x;
				allButtons[y][x].setPreferredSize(new Dimension(70,70));
				setJewelImage(new Coordinate(x,y));
				pane.add(allButtons[y][x], constraint);
			}
		}
		newGameButton.addActionListener(new ActionListener() { 
			  public void actionPerformed(ActionEvent event) { 
			    Gui.gui.game.restartGame();
			    
			  } 
			} );
		pane.add(newGameButton);
		bgPanel.add(pane);
	}
	
	/**
	 * Creates all the buttons and puts them into the button array for the GUI.
	 */
	private void createButtons() {
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				allButtons[x][y] = new JButton();
				allButtons[x][y].addActionListener(this);
				allButtons[x][y].setOpaque(false);
				allButtons[x][y].setContentAreaFilled(false);
				allButtons[x][y].setBorderPainted(false);
			}
		}
	}
	
	/**
	 * Determining which button is pressed.
	 * 
	 * @param event
	 *     ActionEvent to process.
	 */
	public void actionPerformed(ActionEvent event) {
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				if (event.getSource().equals(allButtons[y][x])) {
					Coordinate coord = new Coordinate(x,y);
					game.processJewel(coord);
					return;
				}
			}
		}
	}
	

	
	/**
	 * setJewelImage sets the icon on coordinate (x,y) to the current jewel on that coordinate
	 * 
	 * @param xvalue
	 *     The x-coordinate of the jewel on the board.
	 * @param yvalue
	 *     The y-coordinate of the jewel on the board.
	 */
	public void setJewelImage(Coordinate coord) {
		Jewel jewel = game.getBoard().getJewel(coord);
		ImageIcon icon = null;
		if (jewel == null) {
			icon = imgloader.getImage(Colour.Empty);
		} else {
			icon = imgloader.getImage(jewel.getImageColour());
		}
		allButtons[coord.getY()][coord.getX()].setIcon(icon);
	}
	
	public void highLightJewel(Coordinate coord) {
		Colour colour = game.getBoard().getJewel(coord).getImageColour();
		String highLight = colour + "HL";
		Colour hl = Colour.valueOf(highLight);
		ImageIcon icon = imgloader.getImage(hl);
		allButtons[coord.getY()][coord.getX()].setIcon(icon);
	}
	
	public void clearJewelImage(Coordinate coord) {
		ImageIcon icon = imgloader.getImage(Colour.Empty);
		allButtons[coord.getY()][coord.getX()].setIcon(icon);
	}

	public void jewelsSwapped(Coordinate acoord, Coordinate bcoord) {
		SwapJewelAction swapAction = new SwapJewelAction(this, acoord, bcoord);
		executor.submit(swapAction);
	}

	public void boardChanged() {
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				setJewelImage(new Coordinate(x, y));
			}
		}
	}

	public void jewelSelected(Coordinate coord, Coordinate old) {
		highLightJewel(coord);
		if (old != null) {
			setJewelImage(old);
		}
	}
	
	public void playMatchSound() {
		try {
			Gui.soundloader.playSound(Sounds.Match);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the saved Game, if the file with path and name "saveGamePath"
	 * exists and yields a valid game. Returns a new Game otherwise (in case the
	 * file does not exist, or the game is not playable because there is no time
	 * left).
	 * 
	 * @return
	 */
	private static Game loadGame() {
		File file = new File(saveGamePath);
		if (file.exists()) {
			Game game =  xmlParser.readGame(saveGamePath);
			if (!game.gameLost()) {
				return game;
			}
		}
		return new Game();
	}

	@Override
	public void jewelsCleared(List<Coordinate> coordinates) {
		ClearJewelAction clearAction = new ClearJewelAction(this, coordinates);
		executor.submit(clearAction);
	}

	@Override
	public void jewelDropped(Coordinate from, Coordinate to) {
		DropDownAction dropDownAction = new DropDownAction(this,from,to);
		executor.submit(dropDownAction);
		
	}

	@Override
	public void coordinateFilled(Coordinate coordinate) {
		FillJewelAction fillAction = new FillJewelAction(this, coordinate);
		executor.submit(fillAction);	
	}
}
