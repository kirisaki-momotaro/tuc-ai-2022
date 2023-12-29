package main;

public class GameOverException extends Exception {

	public GameOverState state;
	
	public GameOverException(GameOverState state) {
		super(state.toString());
		this.state = state;
	}

}
