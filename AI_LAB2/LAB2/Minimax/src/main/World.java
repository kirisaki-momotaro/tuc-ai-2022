package main;
import java.awt.SystemColor;
import java.util.ArrayList;
import java.util.Random;


public class World
{
	private String[][] board = null;
	private int rows = 7;
	private int columns = 5;
	private int myColor = 0;
	private ArrayList<String> availableMoves = null;
	private int rookBlocks = 3;		// rook can move towards <rookBlocks> blocks in any vertical or horizontal direction
	private int nTurns = 0;
	private int nBranches = 0;
	private int noPrize = 9;
	
	private int whiteScore=0;
	private int blackScore=0;
	private int prevCutoffValue=5;
	
	private int presentsTakenWhite=0;
	private int presentsTakenBlack=0;
	
	private int abPruningOn=1;
	
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
	
	public void setMyColor(int myColor)
	{
		this.myColor = myColor;
	}
	
	
	
	
	
	private void whiteMoves(String[][] simBoard)
	{
		String firstLetter = "";
		String secondLetter = "";
		String move = "";
				
		for(int i=0; i<rows; i++)
		{
			for(int j=0; j<columns; j++)
			{
				firstLetter = Character.toString(simBoard[i][j].charAt(0));
				
				// if it there is not a white chess part in this position then keep on searching
				if(firstLetter.equals("B") || firstLetter.equals(" ") || firstLetter.equals("P"))
					continue;
				
				// check the kind of the white chess part
				secondLetter = Character.toString(simBoard[i][j].charAt(1));
				
				if(secondLetter.equals("P"))	// it is a pawn
				{
					
					// check if it can move one vertical position ahead
					firstLetter = Character.toString(simBoard[i-1][j].charAt(0));
					
					if(firstLetter.equals(" ") || firstLetter.equals("P"))
					{
						move = Integer.toString(i) + Integer.toString(j) + 
							   Integer.toString(i-1) + Integer.toString(j);
						
						availableMoves.add(move);
					}
					
					// check if it can move crosswise to the left
					if(j!=0 && i!=0)
					{
						firstLetter = Character.toString(simBoard[i-1][j-1].charAt(0));						
						if(!(firstLetter.equals("W") || firstLetter.equals(" ") || firstLetter.equals("P"))) {
							move = Integer.toString(i) + Integer.toString(j) + 
									   Integer.toString(i-1) + Integer.toString(j-1);
								
							availableMoves.add(move);
						}											
					}
					
					// check if it can move crosswise to the right
					if(j!=columns-1 && i!=0)
					{
						firstLetter = Character.toString(simBoard[i-1][j+1].charAt(0));
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
						
						firstLetter = Character.toString(simBoard[i-(k+1)][j].charAt(0));
						
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
						
						firstLetter = Character.toString(simBoard[i+(k+1)][j].charAt(0));
						
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
						
						firstLetter = Character.toString(simBoard[i][j-(k+1)].charAt(0));
						
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
						
						firstLetter = Character.toString(simBoard[i][j+(k+1)].charAt(0));
						
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
					// check if it can move upwards
					if((i-1) >= 0)
					{
						firstLetter = Character.toString(simBoard[i-1][j].charAt(0));
						
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
						firstLetter = Character.toString(simBoard[i+1][j].charAt(0));
						
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
						firstLetter = Character.toString(simBoard[i][j-1].charAt(0));
						
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
						firstLetter = Character.toString(simBoard[i][j+1].charAt(0));
						
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
	}
	
	private void blackMoves(String[][] simBoard)
	{
		String firstLetter = "";
		String secondLetter = "";
		String move = "";
				
		for(int i=0; i<rows; i++)
		{
			for(int j=0; j<columns; j++)
			{
				firstLetter = Character.toString(simBoard[i][j].charAt(0));
				
				// if it there is not a black chess part in this position then keep on searching
				if(firstLetter.equals("W") || firstLetter.equals(" ") || firstLetter.equals("P"))
					continue;
				
				// check the kind of the white chess part
				secondLetter = Character.toString(simBoard[i][j].charAt(1));
				
				if(secondLetter.equals("P"))	// it is a pawn
				{
					
					// check if it can move one vertical position ahead
					firstLetter = Character.toString(simBoard[i+1][j].charAt(0));
					
					if(firstLetter.equals(" ") || firstLetter.equals("P"))
					{
						move = Integer.toString(i) + Integer.toString(j) + 
							   Integer.toString(i+1) + Integer.toString(j);
						
						availableMoves.add(move);
					}
					
					// check if it can move crosswise to the left
					if(j!=0 && i!=rows-1)
					{
						firstLetter = Character.toString(simBoard[i+1][j-1].charAt(0));
						
						if(!(firstLetter.equals("B") || firstLetter.equals(" ") || firstLetter.equals("P"))) {
							move = Integer.toString(i) + Integer.toString(j) + 
									   Integer.toString(i+1) + Integer.toString(j-1);
								
							availableMoves.add(move);
						}																	
					}
					
					// check if it can move crosswise to the right
					if(j!=columns-1 && i!=rows-1)
					{
						firstLetter = Character.toString(simBoard[i+1][j+1].charAt(0));
						
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
						
						firstLetter = Character.toString(simBoard[i-(k+1)][j].charAt(0));
						
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
						
						firstLetter = Character.toString(simBoard[i+(k+1)][j].charAt(0));
						
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
						
						firstLetter = Character.toString(simBoard[i][j-(k+1)].charAt(0));
						
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
						
						firstLetter = Character.toString(simBoard[i][j+(k+1)].charAt(0));
						
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
					// check if it can move upwards
					if((i-1) >= 0)
					{
						firstLetter = Character.toString(simBoard[i-1][j].charAt(0));
						
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
						firstLetter = Character.toString(simBoard[i+1][j].charAt(0));
						
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
						firstLetter = Character.toString(simBoard[i][j-1].charAt(0));
						
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
						firstLetter = Character.toString(simBoard[i][j+1].charAt(0));
						
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
	}
	
	
	public double evaluationFunc(String[][] Simboard) {
		double whitePoints=0;
		double blackPoints=0;
		
		double kingValue=30;
		double rookValue=5;
		double pawnValue=1;
		
		
		for(int i=0; i<rows; i++) {
			for(int j=0; j<columns; j++) {
				String pawn=Simboard[i][j];
				if(pawn.length()==2) {
					String pawnColour = Character.toString(pawn.charAt(0));
					String pawnRole = Character.toString(pawn.charAt(1));
					if(pawnColour.equals("W")) {
						if(pawnRole.equals("K")){
							whitePoints+=kingValue;
						}else if(pawnRole.equals("R")) {
							whitePoints+=rookValue;
						}else {
							whitePoints+=pawnValue;
						}
					}
					if(pawnColour.equals("B")) {
						if(pawnRole.equals("K")){
							blackPoints+=kingValue;
						}else if(pawnRole.equals("R")) {
							blackPoints+=rookValue;
						}else {
							blackPoints+=pawnValue;
						}
					}
				}
				
				
			}
			
		}
		//System.out.println(presentsTakenWhite);
		
		return whitePoints+presentsTakenWhite-blackPoints-presentsTakenBlack;
	}
	
	public void getScoreInformation(int wscore,int bscore) {
		whiteScore=wscore;
		blackScore=bscore;
		}
	
	public int cutoffFunc(int minLevelOfSearch,int maxLevelOfSearch) {
		if(myColor==0) {
			if(whiteScore<blackScore) {
				if(prevCutoffValue<maxLevelOfSearch) {
					prevCutoffValue++;
					return prevCutoffValue;
				}				
			}else {
				if(prevCutoffValue>minLevelOfSearch) {
					prevCutoffValue--;
					return prevCutoffValue;
				}
				
			}
		}else {
			if(whiteScore>blackScore) {
				if(prevCutoffValue<maxLevelOfSearch) {
					prevCutoffValue++;
					return prevCutoffValue;
				}				
			}else {
				if(prevCutoffValue>minLevelOfSearch) {
					prevCutoffValue--;
					return prevCutoffValue;
				}
			}
		}
		return prevCutoffValue;
	}
	
	public int getsPresent(String[][] board,int x1, int y1, int x2, int y2,String colour)
	{
		String target="";
		try {
			target = Character.toString(board[x2][y2].charAt(1));
        } catch(StringIndexOutOfBoundsException e) {
        	target="";
        	 
        }
		
		
		
		if(colour.equals("White")) {
			
			
			if(target.equals("P")) {
				
				presentsTakenWhite++;
				//System.out.println(presentsTakenWhite);
				return 1;
			}
		}else {
			if(target.equals("P")) {
				presentsTakenBlack++;
				return 2;
			}
		}

		
		return 0;
		
	}

	
	public String selectAction()
	{
		
		availableMoves = new ArrayList<String>();
		String chosenMove = null;
		
		int minLevelOfSearch=5;
		int maxLevelOfSearch=7;
		//number of presents taken
		presentsTakenWhite=0;
		presentsTakenBlack=0;
		
		
		int maxSearchDepth=cutoffFunc(minLevelOfSearch ,maxLevelOfSearch);
		//TURN AB PRUNING ON/OFF THROURG CORRESPONDING GLOBAL VARIABLE
		double a_initial;
		double b_initial;
		 a_initial=Double.NEGATIVE_INFINITY;
		 b_initial=Double.POSITIVE_INFINITY;
		
		
		
		
		
		
				
		if(myColor == 0) {// I am the white player
			
			this.whiteMoves(board);
			double whiteBestPosibleOutcome=Double.NEGATIVE_INFINITY;
			
			
			String[][]boardCopy=new String[rows][columns];
			
			for(int i=0; i<rows; i++)
				for(int j=0; j<columns; j++)
					boardCopy[i][j] =board[i][j];
			
			for(String move:availableMoves) {			
	
				int x1 = Integer.parseInt(Character.toString(move.charAt(0)));
				int y1 = Integer.parseInt(Character.toString(move.charAt(1)));
				int x2 = Integer.parseInt(Character.toString(move.charAt(2)));
				int y2 = Integer.parseInt(Character.toString(move.charAt(3)));
				
				String[][] newSimBoard=simMakeMove(board, x1, y1, x2, y2);
				
				int presentTaken=getsPresent(board, x1, y1, x2, y2, "White");
				
				double maxReturnedEval=miniMax(newSimBoard, maxSearchDepth, "Black",a_initial,b_initial);
				
				if (presentTaken==1) {
					presentsTakenWhite--;
				}
				if (presentTaken==2) {
					presentsTakenBlack--;
				}
				
				if(maxReturnedEval>whiteBestPosibleOutcome) {
					whiteBestPosibleOutcome= maxReturnedEval;
					chosenMove=move;
					
				}
				
				for(int i=0; i<rows; i++)
					for(int j=0; j<columns; j++)
						board[i][j] =boardCopy[i][j];
			}
		}
			
			
		else {// I am the black player
			this.blackMoves(board);
			double blackBestPosibleOutcome=Double.POSITIVE_INFINITY;
			String[][]boardCopy=new String[rows][columns];
			
			for(int i=0; i<rows; i++)
				for(int j=0; j<columns; j++)
					boardCopy[i][j] =board[i][j];
			
			for(String move:availableMoves) {			
	
				int x1 = Integer.parseInt(Character.toString(move.charAt(0)));
				int y1 = Integer.parseInt(Character.toString(move.charAt(1)));
				int x2 = Integer.parseInt(Character.toString(move.charAt(2)));
				int y2 = Integer.parseInt(Character.toString(move.charAt(3)));
				
				String[][] newSimBoard=simMakeMove(board, x1, y1, x2, y2);
				
				int presentTaken=getsPresent(board, x1, y1, x2, y2, "Black");
				
				double maxReturnedEval=miniMax(newSimBoard, maxSearchDepth, "White",a_initial,b_initial);
				
				if (presentTaken==1) {
					presentsTakenWhite--;
				}
				if (presentTaken==2) {
					presentsTakenBlack--;
				}
				
				if(maxReturnedEval<blackBestPosibleOutcome) {
					blackBestPosibleOutcome= maxReturnedEval;
					chosenMove=move;
					
				}
				for(int i=0; i<rows; i++)
					for(int j=0; j<columns; j++)
						board[i][j] =boardCopy[i][j];
			}
			
		}
			
		
		// keeping track of the branch factor
		nTurns++;
		nBranches += availableMoves.size();
		
		return chosenMove;
	}
	
	
	
	
	public double miniMax(String[][] Simboard,int searchDepth,String playerColour,double a,double b) {
		
		availableMoves = new ArrayList<String>();
		
		if (searchDepth==0) {
			return evaluationFunc(Simboard);
		}
		
		if(playerColour.equals("White")){
			this.whiteMoves(Simboard);
			double whiteBestPosibleOutcome=Double.NEGATIVE_INFINITY;
			
			String[][]boardCopy=new String[rows][columns];
			
			for(int i=0; i<rows; i++)
				for(int j=0; j<columns; j++)
					boardCopy[i][j] =Simboard[i][j];
			
			
			for(String move:availableMoves) {			

				int x1 = Integer.parseInt(Character.toString(move.charAt(0)));
				int y1 = Integer.parseInt(Character.toString(move.charAt(1)));
				int x2 = Integer.parseInt(Character.toString(move.charAt(2)));
				int y2 = Integer.parseInt(Character.toString(move.charAt(3)));
				
				
				
				String[][] newSimBoard=simMakeMove(Simboard, x1, y1, x2, y2);
				
				int presentTaken=getsPresent(Simboard, x1, y1, x2, y2, playerColour);
				
				double maxReturnedEval=miniMax(newSimBoard, searchDepth-1, "Black",a,b);
				
				if (presentTaken==1) {
					presentsTakenWhite--;
				}
				if (presentTaken==2) {
					presentsTakenBlack--;
				}
				
				if(maxReturnedEval>whiteBestPosibleOutcome) {
					whiteBestPosibleOutcome= maxReturnedEval;					
				}
				
				for(int i=0; i<rows; i++)
					for(int j=0; j<columns; j++)
						Simboard[i][j] =boardCopy[i][j];
				
				if(abPruningOn==1) {
					a=Math.max(a,maxReturnedEval);
					if(b<=a) {
						break;
					}
				}
				
				
				
			}
			return whiteBestPosibleOutcome;
					
			
		}else{
			this.blackMoves(Simboard);
			double blackBestPosibleOutcome=Double.POSITIVE_INFINITY;
			

			String[][]boardCopy=new String[rows][columns];
			
			for(int i=0; i<rows; i++)
				for(int j=0; j<columns; j++)
					boardCopy[i][j] =Simboard[i][j];
			
			
			for(String move:availableMoves) {			

				int x1 = Integer.parseInt(Character.toString(move.charAt(0)));
				int y1 = Integer.parseInt(Character.toString(move.charAt(1)));
				int x2 = Integer.parseInt(Character.toString(move.charAt(2)));
				int y2 = Integer.parseInt(Character.toString(move.charAt(3)));
				
				
				
				String[][] newSimBoard=simMakeMove(Simboard, x1, y1, x2, y2);
				//see if a present is taken
				int presentTaken=getsPresent(Simboard, x1, y1, x2, y2, playerColour);
				
				double maxReturnedEval=miniMax(newSimBoard, searchDepth-1, "White",a,b);
				
				if (presentTaken==1) {
					presentsTakenWhite--;
				}
				if (presentTaken==2) {
					presentsTakenBlack--;
				}
				
				if(maxReturnedEval<blackBestPosibleOutcome) {
					blackBestPosibleOutcome=maxReturnedEval;
				}
				for(int i=0; i<rows; i++)
					for(int j=0; j<columns; j++)
						Simboard[i][j] =boardCopy[i][j];
				
				
				if(abPruningOn==1) {
					b=Math.min(b,maxReturnedEval);
					if(b<=a) {
						break;
					}
				}
				
				
			}
			return blackBestPosibleOutcome;
			}			
			
		}
		
	
	
	
	
	public String[][] simMakeMove( String[][] Simboard,int x1, int y1, int x2, int y2) {
		String chesspart = Character.toString(Simboard[x1][y1].charAt(1));
				
				boolean pawnLastRow = false;
				
				// check if it is a move that has made a move to the last line
				if(chesspart.equals("P"))
					if( (x1==rows-2 && x2==rows-1) || (x1==1 && x2==0) )
					{
						Simboard[x2][y2] = " ";	// in a case an opponent's chess part has just been captured
						Simboard[x1][y1] = " ";
						pawnLastRow = true;
					}
				
				// otherwise
				if(!pawnLastRow)
				{
					Simboard[x2][y2] = Simboard[x1][y1];
					Simboard[x1][y1] = " ";
				}
			
		return Simboard;
			
	}
	
	public double getAvgBFactor()
	{
		return nBranches / (double) nTurns;
	}
	
	public void makeMove(int x1, int y1, int x2, int y2, int prizeX, int prizeY)
	{
		String chesspart = Character.toString(board[x1][y1].charAt(1));
		
		boolean pawnLastRow = false;
		
		// check if it is a move that has made a move to the last line
		if(chesspart.equals("P"))
			if( (x1==rows-2 && x2==rows-1) || (x1==1 && x2==0) )
			{
				board[x2][y2] = " ";	// in a case an opponent's chess part has just been captured
				board[x1][y1] = " ";
				pawnLastRow = true;
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
	}
	
}
