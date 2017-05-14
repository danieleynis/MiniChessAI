/*
Author: Daniel Eynis
E-mail: eynis at pdx dot edu

Online resources consulted:
https://docs.oracle.com/javase/7/docs/api/java/util/ArrayList.html
https://docs.oracle.com/javase/7/docs/api/java/util/Scanner.html
http://stackoverflow.com/questions/13942701/take-a-char-input-from-the-scanner
http://stackoverflow.com/questions/17606839/creating-a-set-of-arrays-in-java
http://stackoverflow.com/questions/19388037/converting-characters-to-integers-in-java
http://stackoverflow.com/questions/12940663/does-adding-a-duplicate-value-to-a-hashset-hashmap-replace-the-previous-value
http://stackoverflow.com/questions/16784347/transposition-table-for-game-tree-connect-4
http://wiki.cs.pdx.edu/mc-howto/movegen.html
 */

import java.util.*;

public class MiniChess {
    private char currentTurn; // keeps track of the current player turn 'W' or 'B'
    private int moveNum;
    private static final int cols = 5;
    private static final int rows = 6;
    private HashMap<String, Character> whitePawns = new HashMap<>();
    private HashMap<String, Character> blackPawns = new HashMap<>();

    MiniChess() {
        Scanner input = new Scanner(System.in);

        int moveNum = input.nextInt();
        assert moveNum >= 0;

        String curTurn = input.next();
        if(!curTurn.isEmpty())
            currentTurn = curTurn.charAt(0);
        input.nextLine();
        assert currentTurn == 'W' || currentTurn == 'B';

        ArrayList<String> inputLines = new ArrayList<>();  // read in input lines into an array list

        String lineRead;
        while(input.hasNextLine()) {
            lineRead = input.nextLine();
            if(lineRead.isEmpty())  // one encounter empty line it is end of input, used for console input
                break;
            inputLines.add(lineRead);
        }

        assert inputLines.size() != 0 && inputLines.size() == rows;

        char[] pieceArray;
        for (int i = 0; i < inputLines.size(); ++i){
            pieceArray = inputLines.get(i).toCharArray();
            assert pieceArray.length == cols;
            for (int j = 0; j < cols; ++j){
                char curPos = pieceArray[j];
                char validPc = Character.toLowerCase(curPos);
                assert validPc == '.' || validPc == 'p' || validPc == 'k' || validPc == 'q' || validPc == 'b'
                        || validPc == 'n' || validPc == 'r';

                if(curPos == '.')
                    continue;
                if(Character.isUpperCase(curPos)){
                    whitePawns.put(i + "" + j, curPos);  // add pawn position in format "rowcol" to hash set
                }
                else{
                    blackPawns.put(i + "" + j, curPos);
                }
            }
        }
    }

    public void printBoard(){
        char toPrint;
        for(int i = 0; i < rows; ++i){
            for(int j = 0; j < cols; ++j){
                if(whitePawns.containsKey(i + "" + j))
                    toPrint = whitePawns.get(i + "" + j);
                else if(blackPawns.containsKey(i + "" + j))
                    toPrint = blackPawns.get(i + "" + j);
                else
                    toPrint = '.';
                System.out.print(toPrint);
            }
            System.out.println();
        }
    }

    private int solveBoard(HashMap<String, Character> wPawns, HashMap<String, Character> bPawns){
        return 0;
    }

    /*
       This function will take hash sets of white and black pawns and an int array of size 4 of a move which
       gives the starting position row and column and the position to move to row and column. This function will
       modify the hash sets directly as it assumes copies are saved if necessary.
     */
    private boolean executeMove(HashMap<String, Character> wPawns, HashMap<String, Character> bPawns, int[] move){
        boolean win = false;

        HashMap<String, Character> onMovePawns;
        HashMap<String, Character> waitingPawns;

        if(currentTurn == 'W') {  // figure out who is on move and who is waiting
            onMovePawns = wPawns;
            waitingPawns = bPawns;
        }
        else {
            onMovePawns = bPawns;
            waitingPawns = wPawns;
        }

        String startLoc = move[0] + "" + move[1];
        String endLoc = move[2] + "" + move[3];

        if(waitingPawns.containsKey(endLoc)){  // if the position to move to has an opponent piece remove it
            if(Character.toLowerCase(waitingPawns.get(endLoc)) == 'k')
                win = true;
            waitingPawns.remove(endLoc);
        }

        onMovePawns.put(endLoc, onMovePawns.get(startLoc));  // add the new position for on move side
        onMovePawns.remove(startLoc);  // remove the old position for on move side

        return win;  // return win status
    }

    private int[] decodeMove(String toDecode){
        assert toDecode.length() == 5;
        int[] move = new int[4];
        int[] decoder = new int[]{5, 4, 3, 2, 1, 0};
        move[0] = decoder[Character.getNumericValue(toDecode.charAt(1)) - 1];
        move[1] = toDecode.charAt(0) - 'a';
        move[2] = decoder[Character.getNumericValue(toDecode.charAt(4)) - 1];
        move[3] = toDecode.charAt(3) - 'a';
        return move;
    }

    private String encodeMove(int[] toEncode){
        assert toEncode.length == 4;
        int[] intEncoder = new int[]{6, 5, 4, 3, 2, 1};
        char[] charEncoder = new char[]{'a', 'b', 'c', 'd', 'e'};
        StringBuilder encoded = new StringBuilder();
        encoded.append(charEncoder[toEncode[1]])
                .append(intEncoder[toEncode[0]])
                .append('-')
                .append(charEncoder[toEncode[3]])
                .append(intEncoder[toEncode[2]]);
        return encoded.toString();
    }

    /*
       This function will generate a list of all possible moves for the side that is on move. An ArrayList of
       int arrays will be returned specifying the starting position coordinated and the coordinates of position
       to move to.
     */
    private ArrayList<int[]> generateMoves(HashMap<String, Character> wPawns, HashMap<String, Character> bPawns){
        ArrayList<int[]> moves = new ArrayList<>();
        int row, col;
        HashMap<String, Character> onMove;

        if(currentTurn == 'W')
            onMove = wPawns;
        else
            onMove = bPawns;

        for(String pos : onMove.keySet()){  // for each pawn for the side on move
            row = Character.getNumericValue(pos.charAt(0));  // get the pawn exact coordinates
            col = Character.getNumericValue(pos.charAt(1));
            char pc = onMove.get(pos);
            boolean stopShort = false;
            Character curPc = Character.toLowerCase(pc);
            switch (curPc){
                case 'k':
                    stopShort = true;
                case 'q':
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, 1, 0, stopShort, 1));
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, 1, 1, stopShort, 1));
                    break;
                case 'b':
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, 1, 1, false, 1));
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, 1, 0, true, 0));
                    break;
                case 'r':
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, 1, 0, false, 1));
                    break;
                case 'n':
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, 2, 1, true, 1));
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, 2, -1, true, 1));
                    break;
                case 'p':
                    int dir = -1;
                    if(Character.isLowerCase(pc))
                        dir = 1;
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, dir, 0, true, 0));
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, dir, -1, true, 2));
                    moves.addAll(generatePieceMoves(wPawns, bPawns, row, col, dir, 1, true, 2));
                    break;
            }
        }
        return moves;  // return the list of moves
    }

    private ArrayList<int[]> generatePieceMoves(HashMap<String, Character> wPawns, HashMap<String, Character> bPawns,
                                                    int rowPos, int colPos, int rowToMov, int colToMove,
                                                    boolean stopShort, int captureSet) {
        ArrayList<int[]> moves = new ArrayList<>();

        Character curPc = wPawns.get(rowPos + "" + colPos);
        if(curPc == null)
            curPc = bPawns.get(rowPos + "" + colPos);

        for(int i = 0; i < 4; ++i){
            int rowTemp = rowPos;
            int colTemp = colPos;
            boolean stopShortTemp = stopShort;

            do{
                rowTemp += rowToMov;
                colTemp += colToMove;

                if(rowTemp >= rows || colTemp >= cols || rowTemp < 0 || colTemp < 0)
                    break;

                Character pc = wPawns.get(rowTemp + "" + colTemp);
                if(pc == null)
                    pc = bPawns.get(rowTemp + "" + colTemp);

                if(pc != null){
                    if(Character.isUpperCase(pc) == Character.isUpperCase(curPc) ||
                            Character.isLowerCase(pc) == Character.isLowerCase(curPc))
                        break;
                    if(captureSet == 0)
                        break;
                    stopShortTemp = true;
                }
                else if(captureSet == 2)
                    break;

                moves.add(new int[]{rowPos, colPos, rowTemp, colTemp});
            }while(!stopShortTemp);

            if(Character.toLowerCase(curPc) == 'p')
                break;

            int temp = colToMove;
            colToMove = - rowToMov;
            rowToMov = temp;
        }

        return moves;
    }
}
