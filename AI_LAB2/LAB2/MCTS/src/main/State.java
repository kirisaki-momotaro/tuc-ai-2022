package main;

public class State {
	private String[][] board;
	private String move;
	private float wins;
	private int visitCount;
	private float score;
	public float uct;
	//0=white,1=black
	private int playerColour;
	
	
	


	public State(String[][] board, int visitCount, int score, int playerColour,String move) {
		this.board = board;
		this.visitCount = visitCount;
		this.score = score;
		this.playerColour = playerColour;
		this.move=move;
	}
	
	
	//unexpanded child node
	public State(State parent,String move) {
		this.board = null;
		this.wins = 0;
		this.visitCount = 0;
		this.score = parent.score;
		this.playerColour = 1 - parent.playerColour;
		this.move = move;
	}
	
	
	
	
	
	
	
	public String getMove() {
		return move;
	}











	public void setMove(String move) {
		this.move = move;
	}





	public void updateUCT(int parentVisits) {
		float c = 1.4f;
		uct = (float) (wins/visitCount + c*Math.sqrt(Math.log(parentVisits)/visitCount));
	}





	public String[][] getBoard() {
		return board;
	}
	public void setBoard(String[][] board) {
		this.board = board;
	}
	
	public float getWins() {
		return wins;
	}
	public void setWins(int wins) {
		this.visitCount = wins;
	}
	public int getVisitCount() {
		return visitCount;
	}
	public void setVisitCount(int visitCount) {
		this.visitCount = visitCount;
	}
	public float getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public int getPlayerColour() {
		return playerColour;
	}
	public void setPlayerColour(int playerColour) {
		this.playerColour = playerColour;
	}
	
	public void upVisitCount() {
		this.visitCount++;
	}
	public void upScore(float diff) {
		this.score+=diff;
	}
	public void upWins() {
		this.wins+=1f;
	}
	public void upTies() {
		this.wins+=0.5f;
	}
	
	
	
}
