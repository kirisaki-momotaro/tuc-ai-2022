package main;
import java.util.ArrayList;
import java.util.LinkedList;

public class Node {
	private State state;
	private Node parent;
	private LinkedList<Node> children;
	
	//MCTS
	
	private World world;
	private boolean isFinal;
	
	//TODO temp debug (though might be useful later on for save/loading)
	public int id;
	public static int new_id = 0;
	
	
	private Node(World world, State state, Node parent, LinkedList<Node> children) {
		this.world = world;
		this.state = state;
		this.parent = parent;
		this.children = children;
		
		this.state.uct = Float.POSITIVE_INFINITY;
		this.isFinal = false;
		
		this.id = new_id++;
	}
	
	//unexpanded node -> an unexpanded node has NULL in children field
	private Node(Node parent, String move) {
		this(parent.world,new State(parent.state,move),parent,null);
		
	}
	
	//root node
	public Node(World world) {
		//before white makes any move, the root node is black
		this(world,new State(world.cloneBoard(world.getBoard()),0,0,1,null),null,new LinkedList<Node>());
		//add all children as unexpanded nodes
		//System.out.println("Possible White Openings:");
		ArrayList<String> whiteOpenings = new ArrayList<String>();
		try {
			world.simulateAction(0,whiteOpenings,world.getBoard());
		} catch (GameOverException e) {
			System.out.println("Game is Rigged!");
			e.printStackTrace();
		}
		for(String move : whiteOpenings) {
			children.add(new Node(this,move));
			//System.out.println(move);
		}
			
	}



	public State getState() {
		return state;
	}
	public void setState(State state) {
		this.state = state;
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
	
	//MCTS Methods
	
	public Node getUCTBestChild() {
		//we're assuming children are sorted by descending UCT
		//also, unexpanded children are given a UCT of infinity
		//return children.peekFirst();
		float inc_uct = 0f;
		Node ret = null;
		for(Node child : children) {
			float uct = child.getState().uct;
			if(uct > inc_uct) {
				ret = child;
				inc_uct = uct;
			}
		}
		return ret;
	}
	
	public boolean getIsFinal() {
		return isFinal;
	}
	
	//go down the tree for Selection phase, updating visit count on the way
	//then do the Expansion as well
	public Node select() {
		state.upVisitCount();
		//if the node is an unexpanded child or a final node
		if(children == null) {
			//TODO temp debug
			//System.out.println("Selected node: ("+parent.getState().getPlayerColour()+")");
			//world.printBoard(parent.getState().getBoard());
			
			//System.out.println("EXPANSION:");
			//if it's a final node, just return it
			if(isFinal)
				return this;
			//if it's the new root (the 1st selection phase)

			//if it's an unexpanded child, expand it:
			//give it its state (board)
			state.setBoard(world.cloneBoard(parent.getState().getBoard()));
			//update its board with its move
			float diff = world.makeMove(state.getMove(),state.getBoard());
			//TODO temp debug
			//if(diff == 0f)
			//System.out.println("hey");
			//System.out.println("Expanding node: ("+state.getPlayerColour()+")");
			//world.printBoard(state.getBoard());
			//update its gamescore
			state.upScore(diff);
			//calculate available moves and check if it's a final node
			ArrayList<String> availableMoves = new ArrayList<>();
			try {
				world.simulateAction(1-state.getPlayerColour(), availableMoves, state.getBoard());
			} catch (GameOverException e) {
				isFinal = true;
				return this;
			}
			//give it its own unexpanded children
			children = new LinkedList<Node>();
			for(String move : availableMoves) {
				children.add(new Node(this,move));
			}
			//then return it
			return this;
		}
		return getUCTBestChild().select();
	}
	
	public int adjustChildren(int[][] newPresents, float adjustment) {
		int adjustments = 0;
		int[][] pres = newPresents;
		if(children != null) {
			for(Node child : children) {
				State childState = child.getState();
				float adj = adjustment;
				 
				//see if the child got any of the new presents
				if(pres != null) {
					for(int i=0;i<newPresents.length;i++) {
						/*
						if(pres[i] != null &&
								Character.getNumericValue(childState.getMove().charAt(2)) == pres[0][i] &&
								Character.getNumericValue(childState.getMove().charAt(3)) == pres[1][i]) {
								*/
						if(pres[i] != null) {
							int[] pr = pres[i];
							if(Character.getNumericValue(childState.getMove().charAt(2)) == pr[0] &&
								Character.getNumericValue(childState.getMove().charAt(3)) == pr[1]) {
								//Present got!
								adj += childState.getPlayerColour() == 0 ? 0.95f : -0.95f;
								//Remove from new presents
								pres[i] = null;
							}else if(childState.getBoard() != null){
								//Add the present to the board
								world.writeTileSafe(childState.getBoard(),pr[0],pr[1],"P");
							}
						}
				}
				
				}
				//adjust child score
				child.state.upScore(adj);
				
				adjustments++;
				adjustments += child.adjustChildren(pres,adjustment);
			}
		}
		return adjustments;
	}
	
	public int adjustChildren(int[][] newPresents) {
		int adjustments = 0;
		int[][] pres = newPresents;
		if(children != null) {
			for(Node child : children) {
				State childState = child.getState();
				float adj = 0f;
				 
				//see if the child got any of the new presents
				if(pres != null) {
					for(int i=0;i<newPresents.length;i++) {
						if(pres[i] != null &&
								Character.getNumericValue(childState.getMove().charAt(2)) == pres[i][0] &&
								Character.getNumericValue(childState.getMove().charAt(3)) == pres[i][1]) {
							//Present got!
							adj += childState.getPlayerColour() == 0 ? 0.95f : -0.95f;
							//Remove from new presents
							pres[i] = null;
						}
				}
				
				}
				//adjust child score
				child.state.upScore(adj);
				
				adjustments++;
				adjustments += child.adjustChildren(pres);
			}
		}
		return adjustments;
	}
	
}
