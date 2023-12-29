package main;

import java.util.concurrent.Semaphore;

public class mctsThread extends Thread {

	private Node root;
	private Node originalRoot;

	Node suggestedNode;
	long iterationsPerGame;
	int iterationsPerRound;

	//TODO temp testing
	//private int maxRoundIter;
	//static Semaphore sem = new Semaphore(1);
	//static Semaphore sem2 = new Semaphore(1);

	boolean running;
	boolean paused;
	boolean isPaused;



	public mctsThread(Node root) {
		super("Thread-MCTS_ND");

		this.originalRoot = root;
		this.root = root;
		this.suggestedNode = null;
		this.iterationsPerRound = 0;
		this.iterationsPerGame = 0;

		this.running = true;
		this.paused = false;
		this.isPaused = false;


		//this.rootLocked = false;
		//this.originalRootLocked = false;
	}

	/*
	public mctsThread(Node root, int maxIterations) {
		this(root);
		//this.maxRoundIter = maxIterations;
	}
	*/

	public void run() {
		System.out.println("---MCTS Algorithm Thread starting from Root ID="+root.getID()+"---");
		//TODO Testing main loop
		//Does this make the thread stuck?

		//TODO temp debug
		//Node prev_root = root;

		try {
			//The main MCTS loop
			while(running) {
				/*
				if(iterationsPerRound < maxRoundIter) {
					sem2.acquire();
				}
				*/
				while(/*sem2.tryAcquire()*/ true) {
					//sem2.acquire();
					//sem.acquire();
					//TODO I believe this is the culprit for the freezing
					//Replace with a more sophisticated thread interrupt solution
					while(paused)
						isPaused = true;
					isPaused = false;
					//we are given the correct root and it is expanded.
					//SELECTION & EXPANSION

					/*
					if(prev_root!=root)
						System.out.println("---MCTS: We have rerooted from: ID="+prev_root.getID()+" to: ID="+root.getID()+"---");
					prev_root = root;
					 */

					Node node = root.select();
					//SIMULATION
					int winner = node.simulate();
					//BACKPROPAGATION
					boolean flag = true;
					float totalTurns = 0f;
					long totalVis = 0;
					while(flag) {
						node.upVisits();
						if(winner == node.getPlayerColour())
							node.upWins();
						else if(winner == 2)
							node.upTies();

						if(!node.isFinal()) {
							for(Node child : node.getChildren()) {
								if(child.getChildren()!=null)
									child.updateUCT();
								if(child.getTurnsToGameOver() != Float.POSITIVE_INFINITY) {
									totalVis+=child.getVisitCount();
									totalTurns+=child.getTurnsToGameOver();
								}
							}
							if(totalTurns!=0)
								node.setTurnsToGameOver((totalTurns/(float)totalVis)+1);
						}
						totalTurns++;

						flag = node != root;
						if(flag)
							node = node.getParent();
					}
					iterationsPerRound++;
					iterationsPerGame++;
					//show a status message every some iterations
					if(iterationsPerGame % 10000 == 0)
						report();
					/*
					sem.release();
					if(iterationsPerRound >= maxRoundIter)
						sem2.release();
						*/
				}
				//sem2.release();
			}
		} catch (OutOfMemoryError e) {
			System.out.println("\n---WE HAVE EXHAUSTED OUR MEMORY RESOURCES---");
			System.out.println("Stopping algorithm...");
			this.running = false;
		}



	}

	private void calculateSuggestedNode() {
		//Get the most visited child, as usual
		long maxVisits = -1;
		for(Node child : root.getChildren()) {
			long vis = child.getVisitCount();
			if(vis > maxVisits) {
				suggestedNode = child;
				maxVisits = vis;
			}
		}
		System.out.println("\n---MCTS: Suggesting Node---");
		suggestedNode.printNodeStats();
	}

	public String getSuggestedMove() {

		if(suggestedNode == null)
			calculateSuggestedNode();
		//sem.release();
		return suggestedNode.getMove();
	}

	public long getIterationsPerRound() {
		return this.iterationsPerRound;
	}

	public void setRoot(Node root, boolean isolate) {
		pause();

		this.root = root;
		this.suggestedNode = null;
		this.iterationsPerRound = 0;

		if(isolate) {
			//detach siblings and parent from the current root and make it into the original root
			//(Java's garbage collection will hopefully free the memory accordingly)
			root.isolate();
			originalRoot = root;
		}

		//sem.release();
		//sem2.release();
		unpause();
	}

	public void stopAlgorithm() {
		this.running = false;
	}

	private void pause() {
		this.paused = true;
		while(!isPaused)
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	private void unpause() {
		this.paused = false;
	}

	public void report() {
		System.out.println("\n---MCTS: STATUS REPORT---");
		System.out.println(iterationsPerGame+" loops made this Game");
		System.out.println(iterationsPerRound+" loops made this Round");
		//System.out.println(originalRoot.getVisitCount()+" Visits to Original Root");
	}

}
