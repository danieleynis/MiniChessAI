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
http://wiki.cs.pdx.edu/mc-howto/negamax.html
https://en.wikipedia.org/wiki/Negamax
 */

import java.io.*;
import java.util.*;

public class MiniChess {
    private char currentTurn; // keeps track of the current player turn 'W' or 'B'
    private int moveNum;
    private static final int depthLimit = 6;
    private static final int cols = 5;
    private static final int rows = 6;
    private int[] moveToMake = null;
    private HashMap<String, Character> whitePieces = new HashMap<>();
    private HashMap<String, Character> blackPieces = new HashMap<>();
    private static final HashMap<Character, Integer> pieceValues = new HashMap<>();

    MiniChess() {
        initializePieceValues();

        String defaultBoard =
                              "0 W\n" +
                              "kqbnr\n" +
                              "ppppp\n" +
                              ".....\n" +
                              ".....\n" +
                              "PPPPP\n" +
                              "RNBQK\n";

        Scanner input = new Scanner(defaultBoard);

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
                    whitePieces.put(i + "" + j, curPos);  // add pawn position in format "rowcol" to hash set
                }
                else{
                    blackPieces.put(i + "" + j, curPos);
                }
            }
        }
/*
        findMove();
        System.out.println(curTurn);
        System.out.println(Arrays.toString(moveToMake));
        char[] moveinfo = executeMove(whitePieces, blackPieces, moveToMake);
        printBoard();
        undoMove(whitePieces, blackPieces, moveToMake, moveinfo);
        printBoard();
        */
    }

    public void playOnServer(String offerID) {
        try {
            if(offerID == null)
                return;
            printBoard();
            Client imcs = new Client("imcs.svcs.cs.pdx.edu", "3589", "slowclap", "slowclap");
            char myColor = imcs.accept(offerID, '?');
            if(myColor == 'W'){
                currentTurn = 'W';
                findMove();
                executeMove(whitePieces, blackPieces, moveToMake);
                imcs.sendMove(encodeMove(moveToMake));
                printBoard();
                for(int i = 0; i < 41; ++i){
                    currentTurn = (currentTurn == 'W' ? 'B' : 'W');
                    int[] move = decodeMove(imcs.getMove());
                    executeMove(whitePieces, blackPieces, move);
                    currentTurn = (currentTurn == 'W' ? 'B' : 'W');
                    findMove();
                    executeMove(whitePieces, blackPieces, moveToMake);
                    imcs.sendMove(encodeMove(moveToMake));
                }
            }
            else{
                currentTurn = 'B';
                for(int i = 0; i < 40; ++i){
                    int[] move = decodeMove(imcs.getMove());
                    currentTurn = (currentTurn == 'W' ? 'B' : 'W');
                    executeMove(whitePieces, blackPieces, move);
                    currentTurn = (currentTurn == 'W' ? 'B' : 'W');
                    findMove();
                    executeMove(whitePieces, blackPieces, moveToMake);
                    imcs.sendMove(encodeMove(moveToMake));
                }
            }
            imcs.close();
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    private void initializePieceValues(){
        pieceValues.put('p', 100);
        pieceValues.put('b', 300);
        pieceValues.put('n', 300);
        pieceValues.put('r', 500);
        pieceValues.put('q', 900);
        pieceValues.put('k', 10000000);
    }

    private int valueState(HashMap<String, Character> wPieces, HashMap<String, Character> bPieces){
        int whiteValue = 0;
        int blackValue = 0;
        int netValue;

        for(Character pc : wPieces.values()){
            whiteValue += pieceValues.get(Character.toLowerCase(pc));
        }

        for(Character pc : bPieces.values()){
            blackValue += pieceValues.get(pc);
        }

        if(currentTurn == 'W') {
            netValue = whiteValue - blackValue;
        }
        else {
            netValue = blackValue - whiteValue;
        }

        return netValue;
    }

    private void printBoard(){
        char toPrint;
        for(int i = 0; i < rows; ++i){
            for(int j = 0; j < cols; ++j){
                if(whitePieces.containsKey(i + "" + j))
                    toPrint = whitePieces.get(i + "" + j);
                else if(blackPieces.containsKey(i + "" + j))
                    toPrint = blackPieces.get(i + "" + j);
                else
                    toPrint = '.';
                System.out.print(toPrint);
            }
            System.out.println();
        }
    }

    private void findMove(){
        moveToMake = null;
        ArrayList<int[]> moves = generateMoves(whitePieces, blackPieces);  // get list of moves

        HashMap<String, Character> copyPiecesW = new HashMap<>(whitePieces);  // create copy of white and black pawn hash sets
        HashMap<String, Character> copyPiecesB = new HashMap<>(blackPieces);  // create copy of white and black pawn hash sets

        int minVal = Integer.MAX_VALUE;
        for(int[] move : moves){
            char[] moveInfo = executeMove(copyPiecesW, copyPiecesB, move);
            currentTurn = (currentTurn == 'W' ? 'B' : 'W');
            int val = negamaxSearch(copyPiecesW, copyPiecesB, depthLimit, -(Integer.MAX_VALUE), Integer.MAX_VALUE);
            currentTurn = (currentTurn == 'W' ? 'B' : 'W');
            undoMove(copyPiecesW, copyPiecesB, move, moveInfo);
            if(val <= minVal){
                moveToMake = move;
                minVal = val;
            }
        }
    }

    private int negamaxSearch(HashMap<String, Character> wPieces, HashMap<String, Character> bPieces, int depth,
                                int alpha, int beta){
        if(depth <= 0 || !wPieces.containsValue('K') || !bPieces.containsValue('k'))
            return valueState(wPieces, bPieces);

        ArrayList<int[]> moves = generateMoves(wPieces, bPieces);  // get list of moves

        if(moves == null || moves.size() == 0) {  // if no moves left, signifies a loss for side on move
            return -10000000;
        }

        int bestValue = Integer.MIN_VALUE;
        int val;
        for(int[] move : moves){
            char[] moveInfo = executeMove(wPieces, bPieces, move);  // execute move on copies
            currentTurn = (currentTurn == 'W' ? 'B' : 'W');  // flip the current turn before recursive call
            val = - negamaxSearch(wPieces, bPieces, depth-1, -beta, -alpha);  // negate the return value of the recursive call (negamax)
            currentTurn = (currentTurn == 'W' ? 'B' : 'W');  // flip back on recursive return
            undoMove(wPieces, bPieces, move, moveInfo);
            bestValue = Math.max(bestValue, val);
            alpha = Math.max(alpha, val);
            if(alpha >= beta)
                break;
        }

        return bestValue;
    }

    /*
       This function will take hash sets of white and black pawns and an int array of size 4 of a move which
       gives the starting position row and column and the position to move to row and column. This function will
       modify the hash sets directly as it assumes copies are saved if necessary.
     */
    private char[] executeMove(HashMap<String, Character> wPieces, HashMap<String, Character> bPieces, int[] move){
        char[] moveInfo = new char[]{Character.MIN_VALUE, Character.MIN_VALUE};

        HashMap<String, Character> onMovePawns;
        HashMap<String, Character> waitingPawns;

        if(currentTurn == 'W') {  // figure out who is on move and who is waiting
            onMovePawns = wPieces;
            waitingPawns = bPieces;
        }
        else {
            onMovePawns = bPieces;
            waitingPawns = wPieces;
        }

        String startLoc = move[0] + "" + move[1];
        String endLoc = move[2] + "" + move[3];

        if(waitingPawns.containsKey(endLoc)){  // if the position to move to has an opponent piece remove it
            moveInfo[1] = waitingPawns.get(endLoc);
            waitingPawns.remove(endLoc);
        }

        try {
            if (Character.toLowerCase(onMovePawns.get(startLoc)) == 'p') {
                if (move[2] == 0 || move[2] == 5) {
                    if (currentTurn == 'W')
                        onMovePawns.put(endLoc, 'Q');
                    else
                        onMovePawns.put(endLoc, 'q');
                    moveInfo[0] = 'T';
                } else
                    onMovePawns.put(endLoc, onMovePawns.get(startLoc));  // add the new position for on move side
            } else
                onMovePawns.put(endLoc, onMovePawns.get(startLoc));  // add the new position for on move side
            onMovePawns.remove(startLoc);  // remove the old position for on move side
        } catch (NullPointerException e){
            System.out.println(e.getMessage());
        }
        return moveInfo;  // return move info
    }

    private void undoMove(HashMap<String, Character> wPieces, HashMap<String, Character> bPieces, int[] move, char[] moveInfo){
        HashMap<String, Character> onMovePawns;
        HashMap<String, Character> waitingPawns;

        if(currentTurn == 'W') {  // figure out who is on move and who is waiting
            onMovePawns = wPieces;
            waitingPawns = bPieces;
        }
        else {
            onMovePawns = bPieces;
            waitingPawns = wPieces;
        }

        String startLoc = move[0] + "" + move[1];
        String endLoc = move[2] + "" + move[3];

        char movedPiece = onMovePawns.get(endLoc);
        onMovePawns.remove(endLoc);
        if(moveInfo[1] != Character.MIN_VALUE)
            waitingPawns.put(endLoc, moveInfo[1]);

        if(moveInfo[0] == 'T')
            onMovePawns.put(startLoc, currentTurn == 'W' ? 'P' : 'p');
        else
            onMovePawns.put(startLoc, movedPiece);
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
    private ArrayList<int[]> generateMoves(HashMap<String, Character> wPieces, HashMap<String, Character> bPieces){
        ArrayList<int[]> moves = new ArrayList<>();
        int row, col;
        HashMap<String, Character> onMove;

        if(currentTurn == 'W')
            onMove = wPieces;
        else
            onMove = bPieces;

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
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, 1, 0, stopShort, 1));
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, 1, 1, stopShort, 1));
                    break;
                case 'b':
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, 1, 1, false, 1));
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, 1, 0, true, 0));
                    break;
                case 'r':
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, 1, 0, false, 1));
                    break;
                case 'n':
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, 2, 1, true, 1));
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, 2, -1, true, 1));
                    break;
                case 'p':
                    int dir = -1;
                    if(Character.isLowerCase(pc))
                        dir = 1;
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, dir, 0, true, 0));
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, dir, -1, true, 2));
                    moves.addAll(generatePieceMoves(wPieces, bPieces, row, col, dir, 1, true, 2));
                    break;
            }
        }
        return moves;  // return the list of moves
    }

    private ArrayList<int[]> generatePieceMoves(HashMap<String, Character> wPieces, HashMap<String, Character> bPieces,
                                                    int rowPos, int colPos, int rowToMov, int colToMove,
                                                    boolean stopShort, int captureSet) {
        ArrayList<int[]> moves = new ArrayList<>();

        Character curPc = wPieces.get(rowPos + "" + colPos);
        if(curPc == null)
            curPc = bPieces.get(rowPos + "" + colPos);

        for(int i = 0; i < 4; ++i){
            int rowTemp = rowPos;
            int colTemp = colPos;
            boolean stopShortTemp = stopShort;

            do{
                rowTemp += rowToMov;
                colTemp += colToMove;

                if(rowTemp >= rows || colTemp >= cols || rowTemp < 0 || colTemp < 0)
                    break;

                Character pc = wPieces.get(rowTemp + "" + colTemp);
                if(pc == null)
                    pc = bPieces.get(rowTemp + "" + colTemp);

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
