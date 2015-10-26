// Fig. 18.8: TicTacToeServer.java
// This class maintains a game of Tic-Tac-Toe for two client applets.#
package A4V1;
import java.awt.*;
import java.net.*;
import java.io.*;
import javax.swing.*;

public class TicTacToeServer extends JFrame {
   private char[] board;           
   private JTextArea outputArea;
   private Player[] players;
   private ServerSocket server;
   private int currentPlayer;
   private final int PLAYER_X = 0, PLAYER_O = 1;
   private final char X_MARK = 'X', O_MARK = 'O';

   // set up tic-tac-toe server and GUI that displays messages
   public TicTacToeServer()
   {
      super( "Tic-Tac-Toe Server" );

      board = new char[ 9 ]; 
      players = new Player[ 2 ];
      currentPlayer = PLAYER_X;
 
      // set up ServerSocket
      try {
         server = new ServerSocket( 12345, 2 );
      }

      // process problems creating ServerSocket
      catch( IOException ioException ) {
         ioException.printStackTrace();
         System.exit( 1 );
      }

      // set up JTextArea to display messages during execution
      outputArea = new JTextArea();
      getContentPane().add( outputArea, BorderLayout.CENTER );
      outputArea.setText( "Server awaiting connections\n" );

      setSize( 300, 300 );
      setVisible( true );

   } // end TicTacToeServer constructor

   // wait for two connections so game can be played
   public void execute()
   {
      // wait for each client to connect
      for ( int i = 0; i < players.length; i++ ) {

         // wait for connection, create Player, start thread
         try {
            players[ i ] = new Player( server.accept(), i );//array of threads
            players[ i ].start();//runs new thread L211
            // players [1] / go to L175
         }

         // process problems receiving connection from client
         catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit( 1 );
         }
      }//finished this

      // Player X is suspended until Player O connects. 
      // Resume player X now.          
      synchronized ( players[ PLAYER_X ] ) {//this synchronised block for playerx
         players[ PLAYER_X ].setSuspended( false );   //call set suspend = false L278
         players[ PLAYER_X ].notify();// We wake up thread
      }//player x is now in L230
  
   }  // end method execute
   
   // utility method called from other threads to manipulate 
   // outputArea in the event-dispatch thread
   private void displayMessage( final String messageToDisplay ) // called by both threads
   {
      // display message from event-dispatch thread of execution
      SwingUtilities.invokeLater(
         new Runnable() {  // inner class to ensure GUI updates properly

            public void run() // updates outputArea
            {
               outputArea.append( messageToDisplay );
               outputArea.setCaretPosition( 
                  outputArea.getText().length() );
            }

         }  // end inner class

      ); // end call to SwingUtilities.invokeLater
   }

   // Determine if a move is valid. This method is synchronized because 
   // only one move can be made at a time.      //1-9           //[;ayer
   public synchronized boolean validateAndMove( int location, int player )
   {
      boolean moveDone = false;

      // while not current player, must wait for turn
      while ( player != currentPlayer ) { // where is current player? L25 at start
         
         // wait for turn
         try {
            wait();
         }

         // catch wait interruptions
         catch( InterruptedException interruptedException ) {
            interruptedException.printStackTrace();
         }
      }

      // if location not occupied, make move
      if ( !isOccupied( location ) ) {// is there a number in here?
  
         // set move in board array, the mark put it
         board[ location ] = currentPlayer == PLAYER_X ? X_MARK : O_MARK;

         // change current player
         currentPlayer = ( currentPlayer + 1 ) % 2; //

         // let new current player know that move occurred, ie other o
         players[ currentPlayer ].otherPlayerMoved( location );// this method L196

         notify(); // tell waiting player to continue
         //

         // tell player that made move that the move was valid
         return true; 
      }

      // tell player that made move that the move was not valid
      else 
         return false;

   } // end method validateAndMove

   // determine whether location is occupied
   public boolean isOccupied( int location )
   {
      if ( board[ location ] == X_MARK || board [ location ] == O_MARK )
          return true;
      else
          return false;
   }

   // place code in this method to determine whether game over 
   public boolean isGameOver()//method outside the 2 threads, has to access the board
   {

      return false;  // this is left as an exercise
   }

   public static void main( String args[] )
   {
      TicTacToeServer application = new TicTacToeServer();
      application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
      application.execute();
   }

   // private inner class Player manages each Player as a thread
   private class Player extends Thread {
      private Socket connection;
      private DataInputStream input;
      private DataOutputStream output;
      private int playerNumber;
      private char mark;
      protected boolean suspended = true;

      // set up Player thread
      public Player( Socket socket, int number )
      {
         playerNumber = number;

         // specify player's mark
         //variable at top
         //player x = 0 and y = 1
         mark = ( playerNumber == PLAYER_X ? X_MARK : O_MARK );// if this is true

         connection = socket;
         
         // obtain streams from Socket
         try {
            input = new DataInputStream( connection.getInputStream() );
            output = new DataOutputStream( connection.getOutputStream() );
         }

         // process problems getting streams
         catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit( 1 );
         }

      } // end Player constructor

      // send message that other player moved
      public void otherPlayerMoved( int location )
      {
         // send message indicating move
         try {
            output.writeUTF( "Opponent moved" );
            output.writeInt( location );
         }

         // process problems sending message
         catch ( IOException ioException ) { 
            ioException.printStackTrace();
         }
      }

      // control thread's execution
      public void run()
      {
         // send client message indicating its mark (X or O),
         // process messages from client
         try {
            displayMessage( "Player " + ( playerNumber == 
               PLAYER_X ? X_MARK : O_MARK ) + " connected\n" );
 
            output.writeChar( mark ); // send player's mark// output = stream

            // send message indicating connection
            // UTF = unicode
            output.writeUTF( "Player " + ( playerNumber == PLAYER_X ? 
               "X connected\n" : "O connected, please wait\n" ) );

            // if player X, wait for another player to arrive
            if ( mark == X_MARK ) {  // Only if we're player x
               output.writeUTF( "Waiting for another player" );
   
               // wait for player O
               try {
                  synchronized( this ) {   //here we go with threading, syncchronised on this object
                     while ( suspended )// variable suspend is set to true, while it's true
                        wait();  //once
                  }
               } 

               // process interruptions while waiting
               catch ( InterruptedException exception ) {
                  exception.printStackTrace();
               }

               // send message that other player connected and
               // player X can make a move
               output.writeUTF( "Other player connected. Your move." );
            }

            // while game not over
            while ( ! isGameOver() ) { //

               // get move location from client
               int location = input.readInt();// read something from stream

               // check for valid move
               if ( validateAndMove( location, playerNumber ) ) { // validate and move L98
                  displayMessage( "\nlocation: " + location );
                  output.writeUTF( "Valid move." );
               }
               else 
                  output.writeUTF( "Invalid move, try again" );
            }         

            connection.close(); // close connection to client

         } // end try

         // process problems communicating with client
         catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit( 1 );
         }

      } // end method run

      // set whether or not thread is suspended
      public void setSuspended( boolean status )
      {
         suspended = status;
      }
   
   } // end class Player

} // end class TicTacToeServer

/**************************************************************************
 * (C) Copyright 1992-2003 by Deitel & Associates, Inc. and               *
 * Prentice Hall. All Rights Reserved.                                    *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
