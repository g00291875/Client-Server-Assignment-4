// Fig. 18.9: TicTacToeClient.java
// Client that let a user play Tic-Tac-Toe with another across a network.
package A4V12;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class TicTacToeClient extends JApplet  implements Runnable{
   private JTextField idField;
   private JTextArea displayArea;
   private JPanel boardPanel, panel2;
   private Square board[][], currentSquare;
   private Socket connection;
   private DataInputStream input;
   private DataOutputStream output;
   private char myMark;
   private boolean myTurn;
   private final char X_MARK = 'X', O_MARK = 'O';

   // Set up user-interface and board
   public void init()// only called once
   {
      Container container = getContentPane();
 
      // set up JTextArea to display messages to user
      displayArea = new JTextArea( 4, 30 );
      displayArea.setEditable( false );
      container.add( new JScrollPane( displayArea ), BorderLayout.SOUTH );

      // set up panel for squares in board
      boardPanel = new JPanel();
      boardPanel.setLayout( new GridLayout( 3, 3, 0, 0 ) );

      // create board
      board = new Square[ 3 ][ 3 ];

      // When creating a Square, the location argument to the constructor 
      // is a value from 0 to 8 indicating the position of the Square on 
      // the board. Values 0, 1, and 2 are the first row, values 3, 4, 
      // and 5 are the second row. Values 6, 7, and 8 are the third row.
      for ( int row = 0; row < board.length; row++ ) {

         for ( int column = 0; column < board[ row ].length; column++ ) {

            // create Square
            board[ row ][ column ] = new Square( ' ', row * 3 + column );
            boardPanel.add( board[ row ][ column ] );        
         }
      }

      // textfield to display player's mark
      idField = new JTextField();
      idField.setEditable( false );
      container.add( idField, BorderLayout.NORTH );
      
      // set up panel to contain boardPanel (for layout purposes)
      panel2 = new JPanel();
      panel2.add( boardPanel, BorderLayout.CENTER );
      container.add( panel2, BorderLayout.CENTER );

   } // end method init

   // Make connection to server and get associated streams.
   // Start separate thread to allow this applet to
   // continually update its output in textarea display.
   public void start()
   {
      // connect to server, get streams and start outputThread
      try {
         
         // make connection         //       host number
         System.out.print("client:" + this.toString()+ " connecting through "+ getCodeBase().getHost() );
         connection = new Socket( getCodeBase().getHost(), 12345 );

         // get streams
         input = new DataInputStream( connection.getInputStream() );
         output = new DataOutputStream( connection.getOutputStream() );
      }

      // catch problems setting up connection and streams
      catch ( IOException ioException ) {
         ioException.printStackTrace();         
      }

      // create and start output thread
      Thread outputThread = new Thread( this );
      outputThread.start();

   } // end method start

   // control thread that allows continuous update of displayArea
   public void run()
   {
      // get player's mark (X or O)
      try {
         myMark = input.readChar();

         // display player ID in event-dispatch thread
         SwingUtilities.invokeLater( 
            new Runnable() {         
               public void run()
               {
                  idField.setText( "You are player \"" + myMark + "\"" );
               }
            }
         ); 
         
         myTurn = ( myMark == X_MARK ? true : false );

         // receive messages sent to client and output them
         while ( connection.isConnected() ) {
            try {
               if(input.available()>0)
                  processMessage( input.readUTF() );//constantly reading processmessage, is it a valid move?
            } catch (IOException e) {
               e.printStackTrace();
            }
         }

      } // end try

      // process problems communicating with server
      catch ( IOException ioException ) {
         ioException.printStackTrace();         
      }

   }  // end method run

   // process messages received by client
   private void processMessage( String message ) {
       switch (message) {
           case "Valid move.":
               displayMessage("Valid move, please wait.\n");
               setMark(currentSquare, myMark);
               break;
           case "Invalid move, try again":
               displayMessage(message + "\n");
               myTurn = true;
               break;
           case "Opponent moved":
               try {
                   updateMove();
                   displayMessage("Opponent moved. Your turn I think.\n");
                   myTurn = true;
               } catch (Exception exception) {
                   exception.printStackTrace();
               }//catch
               break;
//           case "you lose":
//               try {
//                   updateMove();
//                   displayMessage("you lose, do you want to play again?.\n");
//               } catch (Exception e) {
//                   e.printStackTrace();
//               }
           default:
               displayMessage(message + "\n");
       }
   }
//
//         else if ( message.equals( "you lose" ) ) {            /*** YOU LOSE ******/
//         // get move location and update board
//         try {
//            int location = input.readInt();
//            int row = location / 3;
//            int column = location % 3;
//
//            setMark(board[row][column],
//                    (myMark == X_MARK ? O_MARK : X_MARK));
//            displayMessage("Opponent moved. Your turn.\n");
//            myTurn = true;
//            // restart game?
//         }//try
//         catch (IOException ioException) {
//            ioException.printStackTrace();
//         }
//      }//you lose
//            else if ( message.equals( "you win" ) ) {           /*** YOU WIN ******/
//         // get move location and update board
//         try {
//            int location = input.readInt();
//            int row = location / 3;
//            int column = location % 3;
//
//            setMark(board[row][column],
//                    (myMark == X_MARK ? O_MARK : X_MARK));
//            displayMessage("Opponent moved. Your turn.\n");
//            myTurn = true;
//            // restart game?
//         } catch (IOException ioException) {
//            ioException.printStackTrace();
//         }
//      } // you win
//      else if ( message.equals( "you draw" ) ) {                 /*** YOU DRAW ******/
//         // get move location and update board
//         try {
//            int location = input.readInt();
//            int row = location / 3;
//            int column = location % 3;
//
//            setMark(board[row][column],
//                    (myMark == X_MARK ? O_MARK : X_MARK));
//            displayMessage("Opponent moved. Your turn.\n");
//            myTurn = true;
//            // restart game?
//         } catch (IOException ioException) {
//            ioException.printStackTrace();
//         }
//      } // you win

      // simply display message
//      else
//         displayMessage( message + "\n" );

  // } // end method processMessage

   private void updateMove(){
      try {
         int location = input.readInt();
         int row = location / 3;
         int column = location % 3;

         setMark(board[row][column],
                 (myMark == X_MARK ? O_MARK : X_MARK));
        // displayMessage("Opponent moved. Your turn I think.\n");
      }
      catch (IOException io){
         System.out.println("problem updating move");
         io.printStackTrace();
      }
   }

   // utility method called from other threads to manipulate 
   // outputArea in the event-dispatch thread
   private void displayMessage( final String messageToDisplay )
   {
      // display message from event-dispatch thread of execution
      SwingUtilities.invokeLater(
         new Runnable() {  // inner class to ensure GUI updates properly

            public void run() // updates displayArea
            {
               displayArea.append( messageToDisplay );
               displayArea.setCaretPosition( 
                  displayArea.getText().length() );
            }

         }  // end inner class

      ); // end call to SwingUtilities.invokeLater
   }

   // utility method to set mark on board in event-dispatch thread
   private void setMark( final Square squareToMark, final char mark )
   {
      SwingUtilities.invokeLater(
         new Runnable() {
            public void run()
            {
               squareToMark.setMark( mark );
            }
         }
      ); 
   }

   /****************************************************************************
    **********************************************************
    **************************   TO   THE    SERVER
    *****************
    *********/
   // send message to server indicating clicked square
   public void sendClickedSquare( int location )
   {
      if ( myTurn ) {

         // send location to server
         try {
            output.writeInt( location );  /** TO   THE    SERVER**/
            myTurn = false; // now wait
         }

         // process problems communicating with server
         catch ( IOException ioException ) {
            ioException.printStackTrace();
         }
      }
   }

   // set current Square
   public void setCurrentSquare( Square square )
   {
      currentSquare = square;
   }

   // private inner class for the squares on the board
   private class Square extends JPanel { // known only to this.object
      private char mark;
      private int location;
   
      public Square( char squareMark, int squareLocation )
      {
         mark = squareMark;
         location = squareLocation;

         addMouseListener( 
            new MouseAdapter() {// multiple mouse events, has em all
               public void mouseReleased( MouseEvent e )
               {
                  setCurrentSquare( Square.this );//
                  sendClickedSquare( getSquareLocation() );
               }
            }  
         ); 

      } // end Square constructor

      // return preferred size of Square
      public Dimension getPreferredSize() 
      { 
         return new Dimension( 30, 30 );
      }

      // return minimum size of Square
      public Dimension getMinimumSize() 
      {
         return getPreferredSize();
      }

      // set mark for Square
      public void setMark( char newMark ) 
      { 
         mark = newMark; 
         repaint(); 
      }
   
      // return Square location
      public int getSquareLocation() 
      {
         return location; 
      }
   
      // draw Square
      public void paintComponent( Graphics g )
      {
         super.paintComponent( g );//inherit all the things paintComponent can do

         g.drawRect( 0, 0, 29, 29 );
         g.drawString( String.valueOf( mark ), 11, 20 );   
      }

   } // end inner-class Square
 
} // end class TicTacToeClient
