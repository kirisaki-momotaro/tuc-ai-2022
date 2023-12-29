package main;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

//import sz.GameOverException;

public class Node {
	private Node parent;
	//all the children of the node (all variations on one of possible moves AND appearance of a present)
	private LinkedList<Node> children;
	//all the "siblings" of the node (all variations on appearance of a present)
	private LinkedList<Node> siblings;
	//the chance we'll navigate to this sibling as opposed to any other (from the direct parent)
	private int siblingCount;


	private boolean isFinal;

	private String[][] board;
	//0=white,1=black
	//the colour of the player who has JUST completed a move in this node (opposite of the colour of the player next to move)
	private int playerColour;
	//the move that was JUST made to get to this node
	private int[] move;
	//the present that was JUST added after the move
	private int[] present;

	private float wins;
	private long visitCount;
	private float score;
	private float uct;

	private float turnsToGameOver;

	private static Node root = null;

	//TODO temp debug (though might be useful later on for save/loading)
	private long id;
	private static long new_id;

	private static World world;
	//the value for presents/moves that means no present/move
	//must not be a valid coordinate
	private static final int errorValue = 9;

	//new mcts

	//1st (original) root node
	private Node(World world) {
		this.parent = null;
		this.children = new LinkedList<Node>();
		this.siblings = null;
		this.siblingCount = 1;

		this.isFinal = false;

		this.board = world.cloneBoard(world.getBoard());
		this.playerColour = 1;
		this.move = new int[] {errorValue,errorValue,errorValue,errorValue};
		this.present = new int[] {errorValue,errorValue};

		this.wins = 0f;
		this.visitCount = 0;
		this.score = 0;
		this.uct = Float.POSITIVE_INFINITY;
		this.turnsToGameOver = Float.POSITIVE_INFINITY;

		Node.world = world;
		Node.new_id = 0;

		this.id = new_id++;

		String[] availableMoves = {"5040","5141","5242","5343","5444"};
		String[] currentFreeSpots = {"20","21","22","23","24","40","41","42","43","44"};
		//includes the no new present option
		//String[] possibleFreeSpots = findAvailablePresents(availableMoves, currentFreeSpots);

		try {
			addChildren(availableMoves, currentFreeSpots);
		} catch (GameOverException e) {
			System.out.println("Game is rigged!");
			e.printStackTrace();
		}
	}

	//unexpanded child node
	//a node that inherits from its parent, but has no children, and no updated board, score, or final status
	private Node(Node parent, LinkedList<Node> siblings, int siblingCount, String move, String present) {
		this.parent = parent;
		this.children = null;
		siblings.add(this);
		this.siblings = siblings;
		this.siblingCount = siblingCount;

		this.isFinal = false;

		//0=white,1=black
		this.playerColour = 1 - parent.playerColour;
		this.move = new int[4];
		char[] moveArray = move.toCharArray();
		for(int i=0;i<4;i++)
			this.move[i] = Character.getNumericValue(moveArray[i]);
		this.present = new int[2];
		char[] presentArray = present.toCharArray();
		for(int i=0;i<2;i++)
			this.present[i] = Character.getNumericValue(presentArray[i]);

		this.wins = parent.wins;
		this.visitCount = parent.visitCount;

		//we don't give it a board to save time, since it's not bound to be selected
		this.score = parent.score;
		this.uct = Float.POSITIVE_INFINITY;
		this.turnsToGameOver = Float.POSITIVE_INFINITY;

		this.id = new_id++;
	}

	//unexpanded child node
	//a node that inherits from its parent, but has no children, and no updated board, score, or final status
	private Node(Node parent, LinkedList<Node> siblings, int siblingCount, String move, int presentX, int presentY) {
		this.parent = parent;
		this.children = null;
		siblings.add(this);
		this.siblings = siblings;
		this.siblingCount = siblingCount;

		this.isFinal = false;

		//0=white,1=black
		this.playerColour = 1 - parent.playerColour;
		this.move = new int[4];
		char[] moveArray = move.toCharArray();
		for(int i=0;i<4;i++)
			this.move[i] = Character.getNumericValue(moveArray[i]);
		this.present = new int[] {presentX,presentY};

		this.wins = parent.wins;
		this.visitCount = parent.visitCount;

		//we don't give it a board to save time, since it's not bound to be selected
		this.score = parent.score;
		this.uct = Float.POSITIVE_INFINITY;
		this.turnsToGameOver = Float.POSITIVE_INFINITY;

		this.id = new_id++;
	}

	public static Node initRoot(World world) {
		if(Node.root == null)
			Node.root = new Node(world);
		return Node.root;
	}

	//return the move+the present in one string
	public String getDifference() {
		String ret = "";

		ret += Integer.toString(move[0]);
		ret += Integer.toString(move[1]);
		ret += Integer.toString(move[2]);
		ret += Integer.toString(move[3]);

		ret += Integer.toString(present[0]);
		ret += Integer.toString(present[1]);

		return ret;
	}

	public boolean isUnexpanded() {
		return children == null;
	}


	public Node getParent() {
		return parent;
	}
	public void setParent(Node parent) {
		this.parent = parent;
	}
	public LinkedList<Node> getChildren() {
		return children;
	}
	public void setChildren(LinkedList<Node> children) {
		this.children = children;
	}
	public String getMove() {
		String ret = "";
		for(int i=0;i<4;i++)
			ret += Integer.toString(this.move[i]);
		return ret;
	}

	public boolean isFinal() {
		return this.isFinal;
	}

	public void changeScore(float difference) {
		this.score += difference;
	}


	//recursively SELECT and EXPAND nodes from the given one
	//update all stats in backprop phase (to reach to original root)
	public Node select() {
		//if the node is an unexpanded child or a final node:
		//if it's a final node, just return it
		if(isFinal)
			return this;
		//if it's an unexpanded child, expand it:
		if(isUnexpanded()) {
			try {
				//expand it dynamically, from the parent node
				expand(false);
			} catch (GameOverException e) {
				//mark it as a final node and return it
				isFinal = true;
				return this;
			} catch (DebuggerException e) {
				//Dead code zone (this exception should NEVER activate)
				e.printStackTrace();
			}
			//then return it
			return this;
			//NO, RETURN A RANDOM CHILD
			//return stepRandom();
		}
		//if it's an expanded (ie non-leaf) node, navigate to the appropriate child
		return stepUCT().select();
	}


	//EXPAND a node (give it its board, update its score, and give it unexpanded children)
	//staticExpand=false: expand a node based on its move and its parent
	//(slower, may be off depending on possible code mistakes + the 5% chance to not get a present)
	//staticExpand=true: expand a node based on the current state of the world
	//(faster, guaranteed to result in correct child)
	public void expand(boolean staticExpand) throws GameOverException, DebuggerException {
		//Intermediate variables needed to find the node's children
		String[] availableMoves = null;
		String[] currentFreeSpots = null;
		if(staticExpand) {
			if(!world.isUpdated())
				throw new DebuggerException("Copying from outdated world information!");
			this.board = world.cloneBoard();
			this.score = (float)world.getCurrentScore();
			availableMoves = world.cloneAvailableMoves();
			currentFreeSpots = world.cloneCurrentFreeSpots();
		}else {
			String[][] board = world.cloneBoard(parent.board);
			float scoreDiff = world.makeMove(move[0], move[1], move[2], move[3], present[0], present[1], board);
			this.board = board;
			this.score += scoreDiff;
			availableMoves = world.calculateAvailableMoves(1-playerColour, this.board);
			currentFreeSpots = world.calculateFreeSpots(this.board);
		}
		//give it children
		if(availableMoves == null)
			throw new GameOverException(GameOverState.SUFFOCATION);
		children = new LinkedList<Node>();
		String msg = addChildren(availableMoves, currentFreeSpots);
		if(staticExpand)
			System.out.println(msg);
	}


	//SIMULATE from a node (both players doing random actions for now), then return the colour of the winner
	public int simulate() {

		String[][] simBoard = world.cloneBoard(board);
		ArrayList<String> currentFreeSpots = new ArrayList<String>(world.getSize());
		Collections.addAll(currentFreeSpots,world.calculateFreeSpots(simBoard));
		//check if our node is final
		boolean gameIsAfoot = !isFinal;
		//the player next to move is THE OPPOSITE player of the colour of the node (the player who JUST moved)
		int colour = 1 - playerColour;
		float score = this.score; //positive if white is winning, negative if black is
		int winner; //0: white, 1: black, 2: tie
		int turns = 0;
		StringBuilder moveBuilder = new StringBuilder();
		try {
			while(gameIsAfoot) {
				ArrayList<String> simMoves = new ArrayList<String>();
				//Select a move on the simulated board (move selected before exception is thrown, unless it's a suffocation gameover)
				world.simulateAction(colour,simMoves,simBoard,moveBuilder);
				//if we didn't have exception, add a turn
				turns++;
				//Make the move on the simulated board, not adding a present yet
				String move = moveBuilder.toString();
				moveBuilder = new StringBuilder(); //reset the string builder
				float diff = world.makeMove(move,simBoard);
				//update game score
				score += diff;
				//update free spots
				world.updateFreeSpots(currentFreeSpots, move);
				//and possibly add a present on one of them
				world.addPresentMaybe(simBoard, currentFreeSpots);
				//Switch teams
				colour = 1 - colour;
			}
		} catch (GameOverException e) {
			//game is over, after simulation
			//WE STILL NEED TO ADD THE ENDING MOVE'S SCORE! (KING IS WORTH 8 POINTS AND WE JUST IGNORE IT?)
			//THIS MIGHT WELL HAVE BEEN THE BUG I WAS HUNTING THIS WHOLE TIME
			
			//TODO I don't think we need to actually do this. The deadking exception seems to trigger if there is already only 1
			//king on the board, meaning the move was made and the score counted
			if(e.state != GameOverState.SUFFOCATION) {
				String move = moveBuilder.toString();
				score += world.makeMove(move,simBoard);
			}
		}
		//game is over
		//set node's final status and turns to GO (we have not backpropagated yet)
		isFinal = turns == 0;
		if(turnsToGameOver == Float.POSITIVE_INFINITY)
			turnsToGameOver = turns;
		else
			turnsToGameOver = (turnsToGameOver*visitCount+turns)/(float)(visitCount+1);

		if(score>0f)
			//white has won
			winner = 0;
		else if(score<0f)
			//black has won
			winner = 1;
		else
			//tie
			winner = 2;

		return winner;
	}

	//add unexpanded children to a node, return status message
	private String addChildren(String[] availableMoves, String[] currentFreeSpots) throws GameOverException {
		if(availableMoves == null)
			throw new GameOverException(GameOverState.SUFFOCATION);
		int totalChildren = 0;
		int totalFamilies = 0;
		for(String move : availableMoves) {
			totalFamilies++;
			LinkedList<Node> moveSiblings = new LinkedList<Node>();
			int moveSiblingCount = 1;
			if(currentFreeSpots != null) {
				for(String presentSpot : currentFreeSpots) {
					if(!move.substring(2,4).equals(presentSpot))
						moveSiblingCount++;
				}
			}
			//the case where a present doesn't spawn
			this.children.add(new Node(this,moveSiblings,moveSiblingCount,move,errorValue,errorValue));
			//the case where a present spawns on the tile we just left
			this.children.add(new Node(this,moveSiblings,moveSiblingCount,move,move.substring(0,2)));
			//the case where a present spawn on any of the already free spots (other than the one to which we're moving)
			totalChildren+=2;
			for(String presentSpot : currentFreeSpots) {
				if(!move.substring(2,4).equals(presentSpot)) {
					this.children.add(new Node(this,moveSiblings,moveSiblingCount,move,presentSpot));
					totalChildren++;
				}
			}
		}
		return "Node: ID="+id+" Added "+totalChildren+" children among "+totalFamilies+" families.";
	}

	//Update the uct of a node, given its own and the parent's stats (wins/visits are correct)
	public void updateUCT() {
		float c = 1.414f;
		uct = (float) (wins/visitCount + c*Math.sqrt(Math.log(parent.visitCount)/visitCount));
	}


	//Non-deterministic UCT selection
	//from all the children families, get the lowest uct (lower bound) among the children of each one, then get the family with the highest lower bound
	//then get return a sibling of that family at random
	private Node stepUCT() {
		//lists are ordered similarly
		ArrayList<Float> contestantUcts = new ArrayList<Float>();
		ArrayList<Node> coveredFamilyHeads = new ArrayList<Node>();

		for(Node child : children) {
			//if we have not found the contestant from this family
			if(!coveredFamilyHeads.contains(child.siblings.getFirst())) {
				//add the sibling with the lowest uct to the contestants
				float minUct = Float.POSITIVE_INFINITY;
				for(Node sibling : child.siblings)
					if(sibling.uct <= minUct)
						minUct = sibling.uct;
				contestantUcts.add(minUct);
				coveredFamilyHeads.add(child.siblings.getFirst());
			}
		}

		//Choose the contestant index with the highest uct
		int index = 0;
		float maxUct = -1;
		for(int i=0;i<contestantUcts.size();i++) {
			if(contestantUcts.get(i)>maxUct) {
				index = i;
				maxUct = contestantUcts.get(i);
			}
		}

		//choose randomly a sibling from the contestan't family
		Node familyHead = coveredFamilyHeads.get(index);

		return familyHead.siblings.get(world.presentIndex(familyHead.siblingCount));
	}

	//Non-deterministic Random Child
	//from all the children families, get one at random
	//then get return a sibling of that family at random (weighted probability)
	private Node stepRandom() {
		//lists are ordered similarly
		//ArrayList<Float> contestantUcts = new ArrayList<Float>();
		ArrayList<Node> coveredFamilyHeads = new ArrayList<Node>();

		for(Node child : children) {
			//if we have not found the contestant from this family
			if(!coveredFamilyHeads.contains(child.siblings.getFirst())) {
				//add the familyhead
				coveredFamilyHeads.add(child.siblings.getFirst());
			}
		}
		//choose a family randomly (each family corresponds to a move)
		Node familyHead = world.randomNode(coveredFamilyHeads);
		//choose a child from that family randomly (each sibling corresponds to a present variation)
		return familyHead.siblings.get(world.presentIndex(familyHead.siblingCount));
	}

	//return all possible new present locations given the free spots on board at the moment and the possible moves
	@Deprecated
	private static String[] findAvailablePresents(String[] availableMoves, String[] currentFreeSpots) {
		//the most possible locations for a present to appear
		ArrayList<String> ret = new ArrayList<String>(currentFreeSpots.length + availableMoves.length);
		//the possibility of an otherwise free spot being removed due to every single possible move ending in it
		String deadFreeSpot = availableMoves[0].substring(2,4);
		for(String move : availableMoves) {
			ret.add(move.substring(0,1));
			String occupiedSpot = move.substring(2,4);
			if(deadFreeSpot==null || !occupiedSpot.equals(deadFreeSpot))
				deadFreeSpot = null;
		}
		for(String spot : currentFreeSpots)
			ret.add(spot);
		if(deadFreeSpot != null)
			ret.remove(deadFreeSpot);
		//Add the case where no present appears
		ret.add(Integer.toString(errorValue)+Integer.toString(errorValue));

		String[] retTrue = new String[ret.size()];
		return ret.toArray(retTrue);
	}

	//print node information: id, pid, colour (of the player that JUST moved), and board layout
	public void printNode() {
		System.out.println("Node: ID="+this.id+" PID="+(parent!=null ? parent.id : "NULL")+" P"+playerColour);
		System.out.println("Score: "+this.score);
		world.printBoard(board);
	}

	public void printNodeStats() {
		System.out.println("Move: "+this.getMove()+" WVR: "+this.calculateAvgMoveWVR()+"% TtGO: "+(parent.getTurnsToGameOver()-1));
	}

	public int getPlayerColour() {
		return this.playerColour;
	}
	public long getID() {
		return this.id;
	}
	public long getVisitCount() {
		return this.visitCount;
	}
	public float getTurnsToGameOver() {
		return this.turnsToGameOver;
	}
	public void setTurnsToGameOver(float turnsToGameOver) {
		this.turnsToGameOver = turnsToGameOver;
	}

	public void upVisits() {
		this.visitCount++;
	}
	public void upWins() {
		this.wins++;
	}
	public void upTies() {
		this.wins+=0.5f;
	}

	//Method for detaching this subtree from any bigger tree
	public void isolate() {
		//detach siblings
		this.siblings = null;
		//detach parent
		this.parent = null;
		//
		Node.root = this;
	}

	private float calculateAvgMoveWVR() {
		Node sib = siblings.getFirst();
		float ret = sib.visitCount == 0 ? 0 : sib.wins*world.getNoPresentRate()/(float)sib.visitCount;
		float pres = world.getPresentRate();
		int count = sib.siblingCount - 1;
		for(int i=1;i<siblingCount;i++) {
			sib = siblings.get(i);
			if(sib.visitCount != 0)
				ret += sib.wins*pres/(float)(sib.visitCount*count);
		}


		return ret;
	}


}
