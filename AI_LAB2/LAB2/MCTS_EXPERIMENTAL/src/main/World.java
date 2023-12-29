package main;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class World
{
	private String[][] board = null;
	private int rows = 7;
	private int columns = 5;
	private int myColour = 0;
	//all the possible moves we can make
	private ArrayList<String> availableMoves = null;
	private int rookBlocks = 3;		// rook can move towards <rookBlocks> blocks in any vertical or horizontal direction
	private int nTurns = 0;
	private int nBranches = 0;
	private int noPrize = 9;
	//the current score (+for white, -for black)
	private int currentScore = 0;
	//all the tiles on the board that are not currently not occupied
	private ArrayList<String> currentFreeSpots = null;
	
	private static final int noPresentRate = 80;
	private static final int presentRate = 20;
	
	//algorithm settings:
	//TODO Tune to practical values after testing
	//The algorithm we're using to make our moves
	private AlgorithmChoice algorithm = AlgorithmChoice.MCTS_NONDETERMINISTIC;
	//The resource constraint(s) we place on our algorithm
	private ResourceConstraint constraint = ResourceConstraint.TIME;
	//max time per move in ms
	private int maxTime = 600;
	//max iterations per turn
	private int maxIterations = 3;
	
	private mctsThread mcts;
	private Random randy;

	public World()
	{
		board = new String[rows][columns];

		/* represent the board

		BP|BR|BK|BR|BP
		BP|BP|BP|BP|BP
		--|--|--|--|--
		P |P |P |P |P 
		--|--|--|--|--
		WP|WP|WP|WP|WP
		WP|WR|WK|WR|WP
		 */

		// initialization of the board
		for(int i=0; i<rows; i++)
			for(int j=0; j<columns; j++)
				board[i][j] = " ";

		// setting the black player's chess parts

		// black pawns
		for(int j=0; j<columns; j++)
			board[1][j] = "BP";

		board[0][0] = "BP";
		board[0][columns-1] = "BP";

		// black rooks
		board[0][1] = "BR";
		board[0][columns-2] = "BR";

		// black king
		board[0][columns/2] = "BK";

		// setting the white player's chess parts

		// white pawns
		for(int j=0; j<columns; j++)
			board[rows-2][j] = "WP";

		board[rows-1][0] = "WP";
		board[rows-1][columns-1] = "WP";

		// white rooks
		board[rows-1][1] = "WR";
		board[rows-1][columns-2] = "WR";

		// white king
		board[rows-1][columns/2] = "WK";

		// setting the prizes
		for(int j=0; j<columns; j++)
			board[rows/2][j] = "P";

		availableMoves = new ArrayList<String>();
		availableMovesUpdated = false;
		currentFreeSpots = new ArrayList<String>();
		currentFreeSpotsUpdated = false;
		currentScoreUpdated = true;
		
		//initialise the RNG
		this.randy = new Random();

		//initialise the non-deterministic tree
		mctsInit();
		mcts = new mctsThread(root);
		if(algorithm == AlgorithmChoice.MCTS_NONDETERMINISTIC) {
			
			mcts.start();
		}
		
	}
	
	//interface with client
	
	//initialise (response to GB Message)
	public void initialise() throws DebuggerException {
		//start the timer
		moveCountdown();
		
		switch(algorithm) {
		case MCTS_DETERMINISTIC:
			//TODO Integrate old mcts
			throw new DebuggerException("Algorithm not yet integrated!");
		case MCTS_NONDETERMINISTIC:
			//do nothing else
			//throw new DebuggerException("Algorithm not yet base-tested!");
		case RANDOM_MOVE:
			//do nothing else
			return;
		default:
			throw new DebuggerException("Unrecognised Algorithm!");
		}
	}
	
	//update (response to any T message)
	public void update(String difference) {
		System.out.println("\n---NEW ROUND---");
		
		//update the non-deterministic tree
		try {
			treeUpdate(difference);
			mcts.setRoot(root,false);
			//TODO Testing the pruning feature
			//System.out.println("---Chopping Tree---");
		} catch (DebuggerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//
	
	//select REAL action (response to to T message that asks our move)
	public String selectAction() throws DebuggerException
	{
		System.out.println("---ALGORITHM START---");
		printRemainingTime();
		
		updateAvailableMoves(myColour);
		//TODO temp debug
		/*
		System.out.println("Updated available moves:");
		for(String move : availableMoves) {
			System.out.println(move);
		}
		*/
		
		String ret = "";
		

		switch(algorithm) {
		case MCTS_DETERMINISTIC:
			//TODO Integrate old mcts
			throw new DebuggerException("Algorithm not yet integrated!");
		case MCTS_NONDETERMINISTIC:
			//first we wait until the timer is up (polling):
			try {
				switch(constraint) {
				case TIME:
					Thread.sleep(getRemainingTime()-10);
					break;
				case ITERATIONS:
					
					/*
					do {
						Thread.sleep(1);
					}while(mcts.getIterationsPerRound() < maxIterations);
					*/
					break;
				case TIME_AND_ITERATIONS:
					do {
						Thread.sleep(200);
					}while(mcts.getIterationsPerRound() <= maxIterations && getRemainingTime() > 100);
					break;
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mcts.report();
			
			ret = mcts.getSuggestedMove();
			System.out.print("\nAlpha-MCTS (ND) Algorithm chooses move: ");
			break;
			//throw new DebuggerException("Algorithm not yet base-tested!");
		case RANDOM_MOVE:
			ret = this.selectRandomAction(availableMoves);
			System.out.print("\nRandom Moves Algorithm chooses move: ");
			break;
		default:
			throw new DebuggerException("Unrecognised Algorithm!");
		}
		System.out.println(ret);
		//If we're trying to make an illegal move
		if(!this.availableMoves.contains(ret)) {
			emergencyThreadStop();
			throw new DebuggerException("Illegal move chosen!");
		}
		printRemainingTime();
		return ret;
	}
	
	public void gameEnd() {
		emergencyThreadStop();
	}
	
	private void emergencyThreadStop() {
		if(algorithm == AlgorithmChoice.MCTS_NONDETERMINISTIC) {
			mcts.stopAlgorithm();
		}
	}

	//throw exception if the game is over
	//needs to be fed an EMPTY string as the move
	public void simulateAction(int colour, ArrayList<String> availableMoves, String[][] board, StringBuilder move) throws GameOverException
	{
		int m = 0;
		if(colour == 0)		// I am the white player
			m = this.whiteMoves(availableMoves,board);
		else					// I am the black player
			m = this.blackMoves(availableMoves,board);
		if(availableMoves.isEmpty())
			//It can and does happen even though the given rules don't define how we have to proceed when it does
			//So for now at least, I treat as a game over state
			//(since a move can't be made and we can't choose to not make a move, we have to wait out the 14 minutes)
			throw new GameOverException(GameOverState.SUFFOCATION);
		
		//if there is a move we can make, write it, even if it's a gameover (we need to get the score increase!) (dead kings count!)
		//move = this.selectRandomAction(availableMoves);
		move.append(this.selectRandomAction(availableMoves));
		
		if(m == 0)
			throw new GameOverException(GameOverState.DEADKING);
		
		if(m == 1) {
			ArrayList<String> testMoves = new ArrayList<String>();
			String[][] testBoard = cloneBoard(board);
			int p = colour == 0 ? this.blackMoves(testMoves, testBoard) : this.whiteMoves(testMoves, testBoard);
			if(p == 1)
				throw new GameOverException(GameOverState.DEADARMIES);
		}
	}

	//throw exception if the game is over
	/*
	public String simulateAction(State state,ArrayList<String> availableMoves) throws GameOverException
	{
		int colour = state.getPlayerColour();
		String[][] board = state.getBoard();

		return simulateAction(colour, availableMoves, board);
	}
	*/

	//1st return value: 0 if king is dead, 1 if only king is alive, 2 if king and other pieces are alive
	public int whiteMoves(ArrayList<String> availableMoves, String[][] board)
	{
		int ret = 0;
		String firstLetter = "";
		String secondLetter = "";
		String move = "";

		for(int i=0; i<rows; i++)
		{
			for(int j=0; j<columns; j++)
			{
				firstLetter = Character.toString(board[i][j].charAt(0));

				// if it there is not a white chess part in this position then keep on searching
				if(firstLetter.equals("B") || firstLetter.equals(" ") || firstLetter.equals("P"))
					continue;
				ret = ret == 1 ? 2 : ret;

				// check the kind of the white chess part
				secondLetter = Character.toString(board[i][j].charAt(1));

				if(secondLetter.equals("P"))	// it is a pawn
				{

					// check if it can move one vertical position ahead
					firstLetter = Character.toString(board[i-1][j].charAt(0));

					if(firstLetter.equals(" ") || firstLetter.equals("P"))
					{
						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i-1) + Integer.toString(j);

						availableMoves.add(move);
					}

					// check if it can move crosswise to the left
					if(j!=0 && i!=0)
					{
						firstLetter = Character.toString(board[i-1][j-1].charAt(0));						
						if(!(firstLetter.equals("W") || firstLetter.equals(" ") || firstLetter.equals("P"))) {
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i-1) + Integer.toString(j-1);

							availableMoves.add(move);
						}											
					}

					// check if it can move crosswise to the right
					if(j!=columns-1 && i!=0)
					{
						firstLetter = Character.toString(board[i-1][j+1].charAt(0));
						if(!(firstLetter.equals("W") || firstLetter.equals(" ") || firstLetter.equals("P"))) {

							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i-1) + Integer.toString(j+1);							
							availableMoves.add(move);
						}
					}
				}
				else if(secondLetter.equals("R"))	// it is a rook
				{
					// check if it can move upwards
					for(int k=0; k<rookBlocks; k++)
					{
						if((i-(k+1)) < 0)
							break;

						firstLetter = Character.toString(board[i-(k+1)][j].charAt(0));

						if(firstLetter.equals("W"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i-(k+1)) + Integer.toString(j);

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("B") || firstLetter.equals("P"))
							break;
					}

					// check if it can move downwards
					for(int k=0; k<rookBlocks; k++)
					{
						if((i+(k+1)) == rows)
							break;

						firstLetter = Character.toString(board[i+(k+1)][j].charAt(0));

						if(firstLetter.equals("W"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i+(k+1)) + Integer.toString(j);

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("B") || firstLetter.equals("P"))
							break;
					}

					// check if it can move on the left
					for(int k=0; k<rookBlocks; k++)
					{
						if((j-(k+1)) < 0)
							break;

						firstLetter = Character.toString(board[i][j-(k+1)].charAt(0));

						if(firstLetter.equals("W"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i) + Integer.toString(j-(k+1));

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("B") || firstLetter.equals("P"))
							break;
					}

					// check of it can move on the right
					for(int k=0; k<rookBlocks; k++)
					{
						if((j+(k+1)) == columns)
							break;

						firstLetter = Character.toString(board[i][j+(k+1)].charAt(0));

						if(firstLetter.equals("W"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i) + Integer.toString(j+(k+1));

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("B") || firstLetter.equals("P"))
							break;
					}
				}
				else // it is the king
				{
					//note that the white king is alive
					ret = 1;
					// check if it can move upwards
					if((i-1) >= 0)
					{
						firstLetter = Character.toString(board[i-1][j].charAt(0));

						if(!firstLetter.equals("W"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i-1) + Integer.toString(j);

							availableMoves.add(move);	
						}
					}

					// check if it can move downwards
					if((i+1) < rows)
					{
						firstLetter = Character.toString(board[i+1][j].charAt(0));

						if(!firstLetter.equals("W"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i+1) + Integer.toString(j);

							availableMoves.add(move);	
						}
					}

					// check if it can move on the left
					if((j-1) >= 0)
					{
						firstLetter = Character.toString(board[i][j-1].charAt(0));

						if(!firstLetter.equals("W"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i) + Integer.toString(j-1);

							availableMoves.add(move);	
						}
					}

					// check if it can move on the right
					if((j+1) < columns)
					{
						firstLetter = Character.toString(board[i][j+1].charAt(0));

						if(!firstLetter.equals("W"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i) + Integer.toString(j+1);

							availableMoves.add(move);	
						}
					}
				}			
			}	
		}
		return ret;
	}

	//1st return value: 0 if king is dead, 1 if only king is alive, 2 if king and other pieces are alive
	private int blackMoves(ArrayList<String> availableMoves, String[][] board)
	{
		int ret = 0;
		String firstLetter = "";
		String secondLetter = "";
		String move = "";

		for(int i=0; i<rows; i++)
		{
			for(int j=0; j<columns; j++)
			{
				firstLetter = Character.toString(board[i][j].charAt(0));

				// if it there is not a black chess part in this position then keep on searching
				if(firstLetter.equals("W") || firstLetter.equals(" ") || firstLetter.equals("P"))
					continue;
				ret = ret == 1 ? 2 : ret;

				// check the kind of the white chess part
				secondLetter = Character.toString(board[i][j].charAt(1));

				if(secondLetter.equals("P"))	// it is a pawn
				{

					// check if it can move one vertical position ahead
					firstLetter = Character.toString(board[i+1][j].charAt(0));

					if(firstLetter.equals(" ") || firstLetter.equals("P"))
					{
						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i+1) + Integer.toString(j);

						availableMoves.add(move);
					}

					// check if it can move crosswise to the left
					if(j!=0 && i!=rows-1)
					{
						firstLetter = Character.toString(board[i+1][j-1].charAt(0));

						if(!(firstLetter.equals("B") || firstLetter.equals(" ") || firstLetter.equals("P"))) {
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i+1) + Integer.toString(j-1);

							availableMoves.add(move);
						}																	
					}

					// check if it can move crosswise to the right
					if(j!=columns-1 && i!=rows-1)
					{
						firstLetter = Character.toString(board[i+1][j+1].charAt(0));

						if(!(firstLetter.equals("B") || firstLetter.equals(" ") || firstLetter.equals("P"))) {
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i+1) + Integer.toString(j+1);

							availableMoves.add(move);
						}



					}
				}
				else if(secondLetter.equals("R"))	// it is a rook
				{
					// check if it can move upwards
					for(int k=0; k<rookBlocks; k++)
					{
						if((i-(k+1)) < 0)
							break;

						firstLetter = Character.toString(board[i-(k+1)][j].charAt(0));

						if(firstLetter.equals("B"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i-(k+1)) + Integer.toString(j);

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("W") || firstLetter.equals("P"))
							break;
					}

					// check if it can move downwards
					for(int k=0; k<rookBlocks; k++)
					{
						if((i+(k+1)) == rows)
							break;

						firstLetter = Character.toString(board[i+(k+1)][j].charAt(0));

						if(firstLetter.equals("B"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i+(k+1)) + Integer.toString(j);

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("W") || firstLetter.equals("P"))
							break;
					}

					// check if it can move on the left
					for(int k=0; k<rookBlocks; k++)
					{
						if((j-(k+1)) < 0)
							break;

						firstLetter = Character.toString(board[i][j-(k+1)].charAt(0));

						if(firstLetter.equals("B"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i) + Integer.toString(j-(k+1));

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("W") || firstLetter.equals("P"))
							break;
					}

					// check of it can move on the right
					for(int k=0; k<rookBlocks; k++)
					{
						if((j+(k+1)) == columns)
							break;

						firstLetter = Character.toString(board[i][j+(k+1)].charAt(0));

						if(firstLetter.equals("B"))
							break;

						move = Integer.toString(i) + Integer.toString(j) + 
								Integer.toString(i) + Integer.toString(j+(k+1));

						availableMoves.add(move);

						// prevent detouring a chesspart to attack the other
						if(firstLetter.equals("W") || firstLetter.equals("P"))
							break;
					}
				}
				else // it is the king
				{
					//note that the black king is alive
					ret = 1;
					// check if it can move upwards
					if((i-1) >= 0)
					{
						firstLetter = Character.toString(board[i-1][j].charAt(0));

						if(!firstLetter.equals("B"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i-1) + Integer.toString(j);

							availableMoves.add(move);	
						}
					}

					// check if it can move downwards
					if((i+1) < rows)
					{
						firstLetter = Character.toString(board[i+1][j].charAt(0));

						if(!firstLetter.equals("B"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i+1) + Integer.toString(j);

							availableMoves.add(move);	
						}
					}

					// check if it can move on the left
					if((j-1) >= 0)
					{
						firstLetter = Character.toString(board[i][j-1].charAt(0));

						if(!firstLetter.equals("B"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i) + Integer.toString(j-1);

							availableMoves.add(move);	
						}
					}

					// check if it can move on the right
					if((j+1) < columns)
					{
						firstLetter = Character.toString(board[i][j+1].charAt(0));

						if(!firstLetter.equals("B"))
						{
							move = Integer.toString(i) + Integer.toString(j) + 
									Integer.toString(i) + Integer.toString(j+1);

							availableMoves.add(move);	
						}
					}
				}			
			}	
		}
		return ret;
	}



	private String selectRandomAction(ArrayList<String> availableMoves)
	{
		int x = randy.nextInt(availableMoves.size());

		return availableMoves.get(x);
	}

	public double getAvgBFactor()
	{
		return nBranches / (double) nTurns;
	}

	//make a move on a given board (used for simulation)
	//return theoretical score increase of move
	public float makeMove(int x1, int y1, int x2, int y2, int prizeX, int prizeY, String[][] board)
	{
		float ret = 0f;

		int playerColour = board[x1][y1].charAt(0) == 'W' ? 0 : 1;

		String chesspart = Character.toString(board[x1][y1].charAt(1));
		String target = board[x2][y2];
		
		if(!target.equals(" ")) {
			if(target.equals("P"))
				ret = 0.95f;
			else {
				switch(board[x2][y2].charAt(1)) {
				case 'P':
					ret = 1f;
					break;
				case 'R':
					ret = 3f;
					break;
				default:
					ret = 8f;
				}
			}
		}
		boolean pawnLastRow = false;

		// check if it is a move that has made a move to the last line
		if(chesspart.equals("P"))
			if( (x1==rows-2 && x2==rows-1) || (x1==1 && x2==0) )
			{
				board[x2][y2] = " ";	// in a case an opponent's chess part has just been captured
				board[x1][y1] = " ";
				pawnLastRow = true;
				ret += 1f;
			}

		// otherwise
		if(!pawnLastRow)
		{
			board[x2][y2] = board[x1][y1];
			board[x1][y1] = " ";
		}

		// check if a prize has been added in the game
		if(prizeX != noPrize)
			board[prizeX][prizeY] = "P";

		//Score increase is positive for white, negative for black
		return playerColour == 0 ? ret : -ret;
	}

	//make a theoretical move using the ABCD string
	//return theoretical score increase of move
	public float makeMove(String move, String[][] board)
	{
		int x1 = Character.getNumericValue(move.charAt(0));
		int y1 = Character.getNumericValue(move.charAt(1));
		int x2 = Character.getNumericValue(move.charAt(2));
		int y2 = Character.getNumericValue(move.charAt(3)); 
		return makeMove(x1,y1,x2,y2,9,9,board);
	}

	//make a real move on the world board
	//return theoretical score increase of move
	//update to new available moves and free spots as well
	public float makeMove(int colour, int x1, int y1, int x2, int y2, int prizeX, int prizeY)
	{
		float ret = makeMove(x1,y1,x2,y2,prizeX,prizeY,this.board);
		//Given one player JUST made a move, find the available moves of THE OTHER player
		updateAvailableMoves(1-colour);
		updateFreeSpots();
		return ret;
	}

	//MCTS Vars & Methods

	public Node root;
	
	private long moveEndTime;
	
	private boolean availableMovesUpdated;
	private boolean currentFreeSpotsUpdated;
	private boolean currentScoreUpdated;
	
	//Initialise the game tree
	public void mctsInit() {
		//create and expand root node
		root = Node.initRoot(this);
	}
	
	//Navigate and re-root to the appropriate child once a move (ours or enemy's) has been made
	//the tree is non-deterministic, its nodes represent possible new present locations along with each player's moves
	//however, they do not split for the 5% chance to not receive a present. We just add a 0.95 point increase in that case
	//TODO We MAY need to split those children as well. We'll see after mcts is implemented
	public void treeUpdate(String difference) throws DebuggerException {
		//figure out the child that was chosen (both the move and the present variation)
		boolean flg = true;
		for(Node child : root.getChildren()) {
			if(child.getDifference().equals(difference)) {
				//reroot to that child
				root = child;
				flg = false;
				break;
			}
		}
		if(flg)
			throw new DebuggerException("Correct child not found!");
		//expand it if it is an unexpanded child (rare case but technically possible)
		if(root.isUnexpanded())
			try {
				root.expand(true);
				//Dynamic Expansion does work, but the 5% no-get-pres chance can skew results, so we avoid it when possible
				//root.expand(false);
			} catch (GameOverException | DebuggerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//print the new root board:
		System.out.println("New Root:");
		root.printNode();
	}

	//Begin the clock to the end of our turn
	public void moveCountdown() {
		this.moveEndTime = new Date().getTime()+(long)maxTime;
	}
	
	//Get the time remaining before we have to send a move
	public int getRemainingTime() {
		return (int)(moveEndTime - new Date().getTime());
	}
	private int printRemainingTime() {
		int ret = getRemainingTime();
		System.out.println(ret+"ms remain in our round.");
		return ret;
	}
	public void setCurrentScore(int scoreWhite, int scoreBlack) {
		this.currentScore = scoreWhite - scoreBlack;
		this.currentScoreUpdated = true;
	}
	public int getCurrentScore() {
		return this.currentScore;
	}
	public String[][] getBoard(){
		return board;
	}
	public float getNoPresentRate() {
		return (float)noPresentRate/100f;
	}
	public float getPresentRate() {
		return (float)presentRate/100f;
	}
	public boolean isUpdated() {
		return availableMovesUpdated && currentFreeSpotsUpdated && currentScoreUpdated;
	}
	public void setMyColour(int myColour)
	{
		this.myColour = myColour;
	}

	//deep copy any board into any other
	public String[][] cloneBoard(String[][] board){
		String [][] ret = new String[rows][columns];
		for(int i=0;i<rows;i++)
			for(int j=0;j<columns;j++)
				ret[i][j] = board[i][j];
		return ret;
	}
	
	//deep copy the world's board into any other board
	public String[][] cloneBoard() {
		String [][] ret = new String[rows][columns];
		for(int i=0;i<rows;i++)
			for(int j=0;j<columns;j++)
				ret[i][j] = board[i][j];
		return ret;
	}

	//deep copy the world's available moves
	public String[] cloneAvailableMoves() {
		int sz = this.availableMoves.size();
		String ret[] = new String[sz];
		for(int i=0;i<sz;i++)
			ret[i] = this.availableMoves.get(i);
		return ret;
	}

	//deep copy the world's free spots
	public String[] cloneCurrentFreeSpots() {
		int sz = this.currentFreeSpots.size();
		String ret[] = new String[sz];
		for(int i=0;i<sz;i++)
			ret[i] = this.currentFreeSpots.get(i);
		return ret;
	}

	public int getMyColour() {
		return myColour;
	}
	
	public int getSize() {
		return rows*columns;
	}

	/*
	private String[] getAvailableMoves() {
		String[] ret = new String[availableMoves.size()];
		return availableMoves.toArray(ret);
	}
	
	private String[] getCurrentFreeSpots() {
		String[] ret = new String[currentFreeSpots.size()];
		return currentFreeSpots.toArray(ret);
	}
	*/
	
	public void printBoard(String[][] board) {
		int rows = 7;
		int columns = 5;
		for(int i=0;i<rows;i++) {
			String str = i+"|";
			for(int j=0;j<columns;j++)
				str += (board[i][j] + "\t|\t");
			System.out.println(str);
		}
	}
	
	//update current free spots on the board based on current board information (slow way)
	private synchronized void updateFreeSpots() {
		if(currentFreeSpotsUpdated)
			return;
		this.currentFreeSpots = new ArrayList<String>(rows*columns);
		for(int i=0;i<rows;i++)
			for(int j=0;j<columns;j++)
				if(this.board[i][j].equals(" "))
					currentFreeSpots.add(Integer.toString(i)+Integer.toString(j));
		this.currentFreeSpotsUpdated = true;
	}
	
	//calculate free spots on ANY given board
	public String[] calculateFreeSpots(String[][] board) {
		ArrayList<String> freeSpots = new ArrayList<String>(rows*columns);
		for(int i=0;i<rows;i++)
			for(int j=0;j<columns;j++)
				if(board[i][j].equals(" "))
					freeSpots.add(Integer.toString(i)+Integer.toString(j));
		String[] ret = new String[freeSpots.size()];
		return freeSpots.toArray(ret);
	}
	
	//update free spots based on existing free spots and move (fast way) (this is BEFORE any possible appearance of a present)
	public void updateFreeSpots(ArrayList<String> currentFreeSpots, String move) {
		//first remove the end tile of the move from the free spots, if it contains it
		currentFreeSpots.remove(move.substring(2,4));
		//then add the start tile of the move
		currentFreeSpots.add(move.substring(0,2));
	}
	

	//update current available moves BY ANY PLAYER based on current board information
	private synchronized void updateAvailableMoves(int colour) {
		if(availableMovesUpdated)
			return;
		availableMoves = new ArrayList<String>();

		if(colour == 0)		// I am the white player
			this.whiteMoves(availableMoves,board);
		else					// I am the black player
			this.blackMoves(availableMoves,board);

		availableMovesUpdated = true;

		// keeping track of the branch factor
		nTurns++;
		nBranches += availableMoves.size();
	}
	
	//calculate available moves by ANY player on ANY given board
	public String[] calculateAvailableMoves(int colour, String[][] board) {
		ArrayList<String> availableMoves = new ArrayList<String>();

		if(colour == 0)		// I am the white player
			this.whiteMoves(availableMoves,board);
		else					// I am the black player
			this.blackMoves(availableMoves,board);
		String [] ret = new String[availableMoves.size()];
		return availableMoves.toArray(ret);
	}

	public void setMyColor(int myColour) {
		this.myColour = myColour;
	}
	
	//start a new turn by starting the clock and enabling failsafes
	public void newTurn() {
		moveCountdown();
		
		availableMovesUpdated = false;
		currentFreeSpotsUpdated = false;
		currentScoreUpdated = false;
	}
	
	//decide randomly if a present is going to appear or not, and if it does, where it's going to appear from a list
	//0: no present appears (sibling index 0, always)
	//1 to (siblingCount-1): present appears in sibling indexed the return value
	public int presentIndex(int siblingCount) {
		//first roll once to decide if a present appears at all
		int dec = randy.nextInt(100);
		if(dec<noPresentRate)
			return 0;
		//then roll once to decide which of the other siblings we choose
		return randy.nextInt(1,siblingCount);
	}
	
	//return a node randomly from a list of nodes (use for different families)
	public Node randomNode(List<Node> nodes) {
		return nodes.get(randy.nextInt(nodes.size()));
	}
	
	//Given ANY board and ALREADY CALCULATED list of free spots for it, possibly add a present in one of them
	public void addPresentMaybe(String[][] board, ArrayList<String> freeSpots) {
		int index = presentIndex(freeSpots.size()+1); // we're including the no present option
		if(index == 0)
			return;
		//remove that spot from the list of free spots (it will have a present on it from now on)
		String spot = freeSpots.remove(index-1);
		int x = Character.getNumericValue(spot.charAt(0));
		int y = Character.getNumericValue(spot.charAt(1));
		try {
			writeTileSafe(board,x,y,"P");
		} catch (DebuggerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//Overwrite a tile on ANY board if it's empty
	public void writeTileSafe(String[][] board, int x, int y, String newTile) throws DebuggerException {
		if(!board[x][y].equals(" "))
			throw new DebuggerException("Writing on non-empty spot!");
		board[x][y] = newTile;
	}


}
