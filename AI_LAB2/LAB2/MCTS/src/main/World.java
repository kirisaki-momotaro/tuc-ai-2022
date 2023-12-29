package main;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


public class World
{
	private String[][] board = null;
	private int rows = 7;
	private int columns = 5;
	private int myColour = 0;
	private ArrayList<String> availableMoves = null;
	private int rookBlocks = 3;		// rook can move towards <rookBlocks> blocks in any vertical or horizontal direction
	private int nTurns = 0;
	private int nBranches = 0;
	private int noPrize = 9;

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


	}

	public void setMyColour(int myColour)
	{
		this.myColour = myColour;
	}

	public String selectAction()
	{
		availableMoves = new ArrayList<String>();

		if(myColour == 0)		// I am the white player
			this.whiteMoves(availableMoves,board);
		else					// I am the black player
			this.blackMoves(availableMoves,board);

		// keeping track of the branch factor
		nTurns++;
		nBranches += availableMoves.size();

		return this.selectRandomAction(availableMoves);
	}

	//throw exception if the game is over
	public String simulateAction(int colour, ArrayList<String> availableMoves, String[][] board) throws GameOverException
	{
		int m = 0;
		if(colour == 0)		// I am the white player
			m = this.whiteMoves(availableMoves,board);
		else					// I am the black player
			m = this.blackMoves(availableMoves,board);
		if(m == 0)
			throw new GameOverException("No King on one side, game over");
		if(availableMoves.isEmpty())
			//It can and does happen even though the given rules don't define how we have to proceed when it does
			//So for now at least, I treat as a game over state
			throw new GameOverException("Suffocation, game over");
		if(m == 1) {
			//System.out.println("Lonely King Warning on "+colour);
			ArrayList<String> testMoves = new ArrayList<String>();
			String[][] testBoard = cloneBoard(board);
			int p = colour == 0 ? this.blackMoves(testMoves, testBoard) : this.whiteMoves(testMoves, testBoard);
			if(p == 1)
				throw new GameOverException("King stand-off, game over");
		}
		return this.selectRandomAction(availableMoves);
	}

	//throw exception if the game is over
	public String simulateAction(State state,ArrayList<String> availableMoves) throws GameOverException
	{
		int colour = state.getPlayerColour();
		String[][] board = state.getBoard();

		return simulateAction(colour, availableMoves, board);
	}

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
		Random ran = new Random();
		int x = ran.nextInt(availableMoves.size());

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
		//TODO we're not accounting for more prizes yet -> We actually are, in between turns. This is not super accurate, 
		return makeMove(x1,y1,x2,y2,9,9,board);
	}

	//make a move
	//return theoretical score increase of move
	public float makeMove(int x1, int y1, int x2, int y2, int prizeX, int prizeY)
	{
		return makeMove(x1,y1,x2,y2,prizeX,prizeY,this.board);
	}

	//MCTS Vars & Methods

	public Node root;
	public int score=0;

	public void mcstInit() {
		//find available starting moves
		selectAction();
		//create root node
		root = new Node(this);

	}

	//argument is the enemy's move to which we're responding
	//NULL if we're whites in 1st round
	//TODO temp debug
	boolean flg = true;
	public String mcstAction(String enemyMove) {

		//Begin the clock
		long timeStart = new Date().getTime();
		long maxTime = 3200;
		//TODO temp debug
		//long maxTime = 1000*10;
		long timeEnd = timeStart+maxTime;
		
		int[][] newPresents = null;

		if(enemyMove != null) {
			//figure out which child of the root the enemy has chosen
			for(Node child : root.getChildren()) {
				if(child.getState().getMove().equals(enemyMove)) {
					//reroot to that child
					root = child;
					flg = false;
					break;
				}
			}
			if(flg) {
				System.out.println("\n---Huh?---\n");
			}
		}
		//Expand it if it's an unexpanded child
		
		if(root.getChildren() == null)
			root.select();
		
		//update available moves
		selectAction();
		
		//Detect if new presents have appeared (at most 2 new presents)
		if(root.getState().getBoard() != null)
			newPresents = detectNewPresents(root.getState().getBoard());

		//copy the true board over to it
		root.getState().setBoard(cloneBoard(board));
		//copy the true score
		float expRootScore = root.getState().getScore();
		System.out.println("\n---Expected score for new root: "+root.getState().getScore());
		root.getState().setScore(score);
		//prune children that have become unreachable
		if(root.getChildren() != null) {
			for(Node child : root.getChildren()) {
				if(!availableMoves.contains(child.getState().getMove()))
					child = null;
			}

		}

		//TODO temp debug
		System.out.println("\n---NEW ROUND---");
		System.out.println("Root is: ("+root.getState().getPlayerColour()+") Score: "+root.getState().getScore());
		boolean haspar = root.getParent() != null; 
		System.out.println("ID="+root.id+" PID="+(haspar?root.getParent().id:"null"));
		printBoard(root.getState().getBoard());
		
		//If new presents have appeared (at most 2), pass them down the children
		//If the predicted score has deviated from the true value, pass that down also

		float scoreError = expRootScore - root.getState().getScore();

		float errorTolerance = 0.8f;
		if(Math.abs(scoreError) >= errorTolerance) {
			//TODO UPDATE THE CHILDRENS' SCORE!!!
			System.out.println("---Deviation detected, adjusting---");
			int adj = root.adjustChildren(newPresents,-scoreError);
			System.out.println("---Adjusted "+adj+" children by "+(new Date().getTime()-timeStart)+"ms---");
		}
		else if(newPresents != null){
			System.out.println("---Presents detected, adjusting---");
			int adj = root.adjustChildren(newPresents);
			System.out.println("---Adjusted "+adj+" children by "+(new Date().getTime()-timeStart)+"ms---");
		}

		int repeatsPerLoop = 100;
		int totalRepeatsDone = 0;
		while(new Date().getTime() < timeEnd) {
			for(int i=0;i<repeatsPerLoop;i++) {
				//SELECTION & EXPANSION:
				//System.out.println("SELECTION");
				//get node from which to simulate
				Node node = root.select();
				State state = node.getState();
				//SIMULATION:
				//System.out.println("SIMULATION");
				//clone the node's board
				String[][] simBoard = cloneBoard(state.getBoard());
				//check if our node is final
				boolean gameIsAfoot = !node.getIsFinal();
				int colour = state.getPlayerColour();
				float score = state.getScore(); //positive if white is winning, negative if black is
				int winner; //0: white, 1: black, 2: tie
				try {
					while(gameIsAfoot) {
						ArrayList<String> simMoves = new ArrayList<String>();
						//Select a move on the simulated board
						String move = simulateAction(colour,simMoves,simBoard);
						//Make the move on the simulated board
						float diff = makeMove(move,simBoard);
						//update game score
						//score = colour == 0 ? score + diff : score - diff;
						score += diff;
						//Switch teams
						colour = 1 - colour;
					}
				} catch (GameOverException e) {
					//game is over, after simulation
				}
				//game is over
				if(score>0f)
					//white has won
					winner = 0;
				else if(score<0f)
					//black has won
					winner = 1;
				else
					//tie
					winner = 2;
				//TODO temp debug
				//System.out.println("Simulation end, winner is "+winner);
				//printBoard(simBoard);

				//BACKPROPAGATION:
				//System.out.println("BACKPROPAGATION:");
				boolean flag = true;
				while(flag) {
					if(winner == state.getPlayerColour())
						state.upWins();
					else if(winner == 2)
						state.upTies();

					if(!node.getIsFinal()) {
						for(Node child : node.getChildren())
							if(child.getChildren()!=null)
								child.getState().updateUCT(state.getVisitCount());
					}

					flag = node != root;
					if(flag) {
						node = node.getParent();
						state = node.getState();
					}
				}
			}
			totalRepeatsDone += repeatsPerLoop;
		}
		//we're done with the calculations
		//get the most visited child
		//MOST VISITED CHILD(REN) MAY BE INACCESSIBLE DUE TO NEW PRESENTS APPEARING, IN WHICH CASE WE GO TO THE MOST VISITED LEGAL CHILD
		int maxVis = -1;
		Node node = null;
		System.out.println("All legal children:");
		for(Node child : root.getChildren()) {
			int vis = child.getState().getVisitCount();
			//TODO temp debug
			float wins = child.getState().getWins();
			System.out.println(
					child.getState().getMove()+"\tVis: "+vis+"\tWins: "+wins+"\tWVR: "+wins/(float)vis+"\tScore: "+child.getState().getScore());
			if(vis > maxVis && availableMoves.contains(child.getState().getMove()))
			{
				node = child;
				maxVis = vis;
			}
		}
		if(node == null)
			System.out.println("\n---Oh no!---\n");
		//re-root the tree
		root = node;
		//TODO temp debug
		System.out.println("\nSelecting child "+node.getState().getMove()+": Estimated "+node.getState().getWins()/(float)maxVis*100+"% Chance of Victory after "+totalRepeatsDone+" loops");
		System.out.println("ID="+root.id+" PID="+root.getParent().id);
		printBoard(node.getState().getBoard());
		assert(root.getState().getPlayerColour() == myColour);
		System.out.println("I made move: "+node.getState().getMove());

		//return the chosen move
		return node.getState().getMove();
	}

	public String[][] getBoard(){
		return board;
	}


	//deep copy
	public String[][] cloneBoard(String[][] board){
		String [][] ret = new String[rows][columns];
		for(int i=0;i<rows;i++)
			for(int j=0;j<columns;j++)
				ret[i][j] = board[i][j];
		return ret;
	}
	
	public void writeTileSafe(String[][] board, int x, int y, String newTile) {
		if(board[x][y].equals(" "))
			board[x][y] = newTile;
	}


	public int getColour() {
		return myColour;
	}

	public String[] getAvailableMoves() {
		String[] ret = new String[availableMoves.size()];
		return availableMoves.toArray(ret);
	}

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
	
	public int[][] detectNewPresents(String[][] oldBoard) {
		int[] present1 = null;
		int[] present2;
		boolean foundPresent = false;
		for(int i=0;i<rows;i++) {
			for(int j=0;j<columns;j++) {
				if(board[i][j].equals("P") && !oldBoard[i][j].equals("P")) {
					if(foundPresent) {
						present2 = new int[] {i,j};
						return new int[][] {present1,present2};
					}
					present1 = new int[] {i,j};
					foundPresent = true;
				}
			}
		}
		return foundPresent ? new int[][] {present1} : null;
	}


}
