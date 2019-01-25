// package chain.reaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javafx.util.Pair;

/**
 * Input an array of 8*8
 *
 * @author Shamiul Hasan
 */
public class ChainReaction {

    static String[][] grid;
    static int[][][] board;
    static int rowNum, colmNum;

    static String player_color;

    public static String[][] read_file() throws FileNotFoundException, IOException {
        File file = new File("shared_file.txt");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        String[][] board = new String[8][8];

        if (file.length() == 0) {
            fr.close();
            br.close();
            return null;
        }
        String color = br.readLine();
//        System.out.println(color + " " + player_color);
        if (color != null && color.equalsIgnoreCase(player_color)) {
            for (int i = 0; i < rowNum; i++) {
                String line = br.readLine();
                String[] arr = line.split(" ");
//                System.out.println("Arr = ");
                for (int j = 0; j < colmNum; j++) {
//                    System.out.print(arr[j] + " ");
                    board[i][j] = arr[j];
                }
//                System.out.println("");
            }
            fr.close();
            br.close();
            return board;
        }
        fr.close();
        br.close();
        return null;
    }

    public static void print() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(grid[i][j] + " ");
            }
            System.out.println("");
        }
    }

    public static Pair<Integer, Integer> select_move(int player_id) throws FileNotFoundException {
        while (true) {
            int x, y;
            
            final MinMax minMax = new MinMax();
            String ans = minMax.iterativeSearchForBestMove(board, player_id);
            String xy[] = ans.split(" "); 
            x = Integer.parseInt(xy[0]); 
            y = Integer.parseInt(xy[1]); 
            
            if (grid[x][y].equalsIgnoreCase("No") || grid[x][y].charAt(0) == player_color.charAt(0)) {
                System.out.println("x = " + x + " y = " + y);
                return new Pair<>(x, y);
            }
        }
    }

    public static void write_move(Pair<Integer, Integer> p) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter("shared_file.txt");
        writer.println("0");
        writer.println(p.getKey() + " " + p.getValue());
        writer.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        rowNum = colmNum = 8;
        grid = new String[rowNum][colmNum];

        player_color = args[0];
//        player_color = "G";

        while (true) {
            while (true) {
                grid = read_file();
                if (grid != null) {
                    break;
                }
                TimeUnit.SECONDS.sleep((long) 0.01);
            }
            
            board = new int[rowNum][colmNum][3];
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {

                    char player = grid[i][j].charAt(0);
                    char orb_count = grid[i][j].charAt(1);
                    int player_id = 0, count;

                    if (player == 'G') {
                        player_id = 1;
                    } else if (player == 'R') {
                        player_id = 2;
                    }

                    count = orb_count - '0';

                    board[i][j][0] = player_id;
                    board[i][j][1] = count;
//                    board[i][j][1] = player_id;
//                    board[i][j][0] = count;
                }
            }

            int my_player_id ; 
            if(player_color.equalsIgnoreCase("R"))
            {
                my_player_id = 2; 
            }
            else {
                my_player_id = 1;
            }
//            select_move(my_player_id);
            Pair<Integer, Integer> p = select_move(my_player_id);
            write_move(p);
        }
    }
}


class MinMax {
    private static final int MAX_DEPTH = 60;
    public static int TIME_OUT = 1280;
    public int computations = 0, depth = 4, moves = 0;
    public long eval = 0;
    static final int MAX_VALUE = 1000000, MIN_VALUE = -MAX_VALUE;
    private final long startTime = System.currentTimeMillis();
    private boolean test;
    private Configuration[] startConfigs;
    private final Move[][] killerMoves = new Move[MAX_DEPTH][2];
    private final int[][] efficiency = new int[MAX_DEPTH][2];
    private boolean nullSearchActivated = false;

    public MinMax() {
        Board.setMoves();
        Board.setNeighbours();
    }

    private boolean timeOut;

    public String iterativeSearchForBestMove(final int[][][] game, final int player) {
        final Board board = new Board(game);
        if (board.choices[player] + board.choices[0] == 0) {
            throw new RuntimeException("No possible moves");
        }
        startConfigs = new Configuration[board.choices[player] + board.choices[0]];
        for (int i = 0; i < board.choices[0]; i++) {
            startConfigs[i] = new Configuration(board.moves[0][i], board, player, 0, false);
        }
        for (int i = 0; i < board.choices[player]; i++) {
            startConfigs[i + board.choices[0]] = new Configuration(board.moves[player][i], board, player, 0, false);
        }
        Arrays.sort(startConfigs);
        Move bestMove = startConfigs[0].move;
        while (depth < MAX_DEPTH && !timeOut) {
            bestMove = findBestMove(player, 0);
            depth++;
        }
        eval = startConfigs[0].strength;
        moves = board.choices[player] + board.choices[0];
        return bestMove.describe();
    }


    private Move findBestMove(final int player, final int level) {
        long toTake = MIN_VALUE, toGive = MAX_VALUE;
        int max = MIN_VALUE;
        Move bestMove = startConfigs[0].move;
        try {
            for (final Configuration possibleConfig : startConfigs) {
                final int moveValue = evaluate(possibleConfig.board.getCopy(),flip(player),level,toTake,
                                               toGive, -possibleConfig.strength,false);
                possibleConfig.strength = moveValue;
                if (player == 1) {
                    if (toTake < moveValue) {
                        toTake = moveValue;
                    }
                } else {
                    if (toGive > -moveValue) {
                        toGive = -moveValue;
                    }
                }
                if (moveValue > max) {
                    max = moveValue;
                    bestMove = possibleConfig.move;
                    if (Math.abs(max - MAX_VALUE) <= 100) {
                        break;
                    }
                }
                if (toTake >= toGive) {
                    if (possibleConfig.killer) {
                        if (killerMoves[level][0] == possibleConfig.move) {
                            efficiency[level][0]++;
                        } else {
                            efficiency[level][1]++;
                            if (efficiency[level][0] < efficiency[level][1]) {
                                final Move temp = killerMoves[level][0];
                                killerMoves[level][0] = killerMoves[level][1];
                                killerMoves[level][1] = temp;
                            }
                        }
                    } else {
                        if (killerMoves[level][0] == null) {
                            killerMoves[level][0] = possibleConfig.move;
                            efficiency[level][0] = 1;
                        } else if (killerMoves[level][1] == null) {
                            killerMoves[level][1] = possibleConfig.move;
                            efficiency[level][1] = 1;
                        }
                    }
                    break;
                } else if (possibleConfig.killer) {
                    if (killerMoves[level][0] == possibleConfig.move) {
                        efficiency[level][0]--;
                    } else {
                        efficiency[level][1]--;
                    }
                    if (efficiency[level][0] < efficiency[level][1]) {
                        final Move temp = killerMoves[level][0];
                        killerMoves[level][0] = killerMoves[level][1];
                        killerMoves[level][1] = temp;
                    }
                    if (efficiency[level][1] <= 0) {
                        efficiency[level][1] = 0;
                        killerMoves[level][1] = null;
                    }
                }
            }
        } catch (TimeoutException e) {
            timeOut = true;
        }
        Arrays.sort(startConfigs);
        return bestMove;
    }

 
    private int evaluate(final Board board,
                         final int player,
                         final int level,
                         final long a,
                         final long b,
                         final int heuristicValue,
                         final boolean isNullSearch) throws TimeoutException {
        long toTake = a, toGive = b;
        int max = MIN_VALUE;
        if (!test && System.currentTimeMillis() - startTime >= TIME_OUT) {
            throw new TimeoutException("Time out...");
        }
        final Integer terminalValue;
        if ((terminalValue = board.terminalValue()) != null) {
            max = terminalValue * ((-player << 1) + 3);
            max += max < 0 ? level : -level;
        } else if (level >= depth) {
            max = heuristicValue;
        } else {
            final Configuration[] configurations = new Configuration[board.choices[player] + board.choices[0]];
            for (int i = 0; i < board.choices[0]; i++) {
                configurations[i] = new Configuration(board.moves[0][i], board, player, level, isNullSearch);
            }
            for (int i = 0; i < board.choices[player]; i++) {
                configurations[i + board.choices[0]] = new Configuration(board.moves[player][i],
                                                                         board,
                                                                         player,
                                                                         level,
                                                                         isNullSearch);
            }
            Arrays.sort(configurations);
            int index = 0;
            for (; index < configurations.length; index++) {
                final Configuration possibleConfig = configurations[index];
                computations++;
                if (nullSearchActivated && !isNullSearch && isNotEndGame(possibleConfig)) {
                    final int nullMoveValue = -evaluate(possibleConfig.board,
                                                        player,
                                                        level + 3,
                                                        player == 1 ? toTake : toGive - 1,
                                                        player == 1 ? toTake + 1 : toGive,
                                                        possibleConfig.strength,
                                                        true);
                    if (player == 1) {
                        if (nullMoveValue <= toTake) {
                            if (nullMoveValue > max) {
                                max = nullMoveValue;
                            }
                            continue;
                        }
                    } else {
                        if (-nullMoveValue >= toGive) {
                            if (nullMoveValue > max) {
                                max = nullMoveValue;
                            }
                            continue;
                        }
                    }
                }
                final int moveValue = evaluate(possibleConfig.board,
                                               flip(player),
                                               level + 1,
                                               toTake,
                                               toGive,
                                               -possibleConfig.strength,
                                               isNullSearch);
                if (player == 1) {
                    if (toTake < moveValue) {
                        toTake = moveValue;
                    }
                } else {
                    if (toGive > -moveValue) {
                        toGive = -moveValue;
                    }
                }
                if (moveValue > max) {
                    max = moveValue;
                    if (Math.abs(max - MAX_VALUE) <= 100) {
                        break;
                    }
                }
                if (toTake >= toGive) {
                    max = moveValue;
                    if (possibleConfig.killer) {
                        if (killerMoves[level][0] == possibleConfig.move) {
                            efficiency[level][0]++;
                        } else {
                            efficiency[level][1]++;
                            if (efficiency[level][0] < efficiency[level][1]) {
                                final Move temp = killerMoves[level][0];
                                killerMoves[level][0] = killerMoves[level][1];
                                killerMoves[level][1] = temp;
                            }
                        }
                    } else {
                        if (killerMoves[level][0] == null) {
                            killerMoves[level][0] = possibleConfig.move;
                            efficiency[level][0] = 1;
                        } else if (killerMoves[level][1] == null) {
                            killerMoves[level][1] = possibleConfig.move;
                            efficiency[level][1] = 1;
                        }
                    }
                    break;
                } else if (possibleConfig.killer) {
                    if (killerMoves[level][0] == possibleConfig.move) {
                        efficiency[level][0]--;
                    } else {
                        efficiency[level][1]--;
                    }
                    if (efficiency[level][0] < efficiency[level][1]) {
                        final Move temp = killerMoves[level][0];
                        killerMoves[level][0] = killerMoves[level][1];
                        killerMoves[level][1] = temp;
                    }
                    if (efficiency[level][1] <= 0) {
                        efficiency[level][1] = 0;
                        killerMoves[level][1] = null;
                    }
                }
            }
        }
        return -max;
    }

    private boolean isNotEndGame(Configuration configuration) {
        return configuration.board.choices[0] > 5;
    }

    /**
     * A board and move combination.
     */
    private class Configuration implements Comparable<Configuration> {
        final Move move;
        final Board board;
        /**
         * Represents how good the move is for the player making the move
         */
        int strength;
        /**
         * True only if the move is considered a 'killer' move as per the killer heuristic.
         */
        final boolean killer;

        private Configuration(final Move move,
                              final Board board,
                              final int player,
                              final int level,
                              boolean resultsFromNullSearch) {
            final Move moveToBeMade = Board.ALL_MOVES[player][move.x][move.y];
            this.board = board.makeMove(moveToBeMade);
            if (!resultsFromNullSearch && killerMoves[level][0] == moveToBeMade || killerMoves[level][1] == moveToBeMade) {
                killer = true;
            } else {
                this.strength = this.board.heuristicValue(player);
                killer = false;
            }
            this.move = moveToBeMade;
        }

        @Override
        public int compareTo(Configuration o) {
            if (killer && o.killer) {
                return 0;
            } else if (!killer && o.killer) {
                return +1;
            } else if (killer) {
                return -1;
            }
            return o.strength - strength;
        }

        @Override
        public String toString() {
            return "Configuration{" +
                    "move=" + move +
                    ", board=" + board +
                    '}';
        }
    }

    static int flip(final int player) {
        return ~player & 3;
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}


class Move {
    final int x, y, player;

    Move(final int x, final int y, final int player) {
        this.x = x;
        this.y = y;
        this.player = player;
    }

    String describe() {
        return x + " " + y;
    }

    @Override
    public String toString() {
        return "Move{" +
                "x=" + x +
                ", y=" + y +
                ", player=" + player +
                '}';
    }
}


class Board {
    Function<int[], Integer> heuristicEval = (vals) -> Arrays.stream(vals).sum();
    int[][][] board;
    private static final int BOARD_SIZE = 8;
    private static final int neighbours[][][] = new int[BOARD_SIZE][BOARD_SIZE][];
    private static final int PLAYERS = 3;
    final Move[][] moves = new Move[PLAYERS][BOARD_SIZE * BOARD_SIZE];
    final int[] choices = new int[PLAYERS];
    static final Move ALL_MOVES[][][] = new Move[PLAYERS][BOARD_SIZE][BOARD_SIZE];

  
    Board(final int[][][] board) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                moves[board[i][j][0]][choices[board[i][j][0]]++] = ALL_MOVES[board[i][j][0]][i][j];
            }
        }
        this.board = getCopy(board);
    }

    private Board(final int[][][] board, final Move[][] moves, final int choices[]) {
        System.arraycopy(choices, 0, this.choices, 0, choices.length);
        for (int i = 0; i < PLAYERS; i++) {
            System.arraycopy(moves[i], 0, this.moves[i], 0, choices[i]);
        }
        this.board = getCopy(board);
    }

 
    static void setNeighbours() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                final long x = i * BOARD_SIZE + j;
                final List<Long> near = new ArrayList<>();
                near.add(x + 1);
                near.add(x + BOARD_SIZE);
                near.add(x - 1);
                near.add(x - BOARD_SIZE);
                if (i == 0) {
                    near.remove(x - BOARD_SIZE);
                }
                if (j == 0) {
                    near.remove(x - 1);
                }
                if (i == BOARD_SIZE - 1) {
                    near.remove(x + BOARD_SIZE);
                }
                if (j == BOARD_SIZE - 1) {
                    near.remove(x + 1);
                }
                neighbours[i][j] = new int[near.size()];
                for (int k = 0; k < near.size(); k++) {
                    if (near.get(k) >= 0 && near.get(k) <= BOARD_SIZE * BOARD_SIZE) {
                        neighbours[i][j][k] = Math.toIntExact(near.get(k));
                    }
                }
            }
        }
    }

    Board makeMove(final Move move) {
        return getCopy().play(move);
    }


    private Board play(final Move move) {
        if (board[move.x][move.y][0] == MinMax.flip(move.player)) {
            //We just captured an opponents block. Updating move list and counters
            final int opponent = MinMax.flip(move.player);
            int index;
            for (index = choices[opponent] - 1; index >= 0; index--) {
                if (moves[opponent][index].x == move.x && moves[opponent][index].y == move.y) {
                    break;
                }
            }
            moves[opponent][index] = moves[opponent][choices[opponent] - 1];
            choices[opponent]--;
            moves[move.player][choices[move.player]++] = ALL_MOVES[move.player][move.x][move.y];
        } else if (board[move.x][move.y][0] == 0) {
            //We just captured an an empty block. Updating move list and counters
            int index;
            for (index = choices[0] - 1; index >= 0; index--) {
                if (moves[0][index].x == move.x && moves[0][index].y == move.y) {
                    break;
                }
            }
            moves[0][index] = moves[0][choices[0] - 1];
            choices[0]--;
            moves[move.player][choices[move.player]++] = ALL_MOVES[move.player][move.x][move.y];
        }
        //Else we played in our own cell. No updates needed, except to increment cell count as always
        board[move.x][move.y][0] = move.player;
        board[move.x][move.y][1]++;
        if (terminalValue() != null) {
            return this;
        }
      
        if (neighbours[move.x][move.y].length <= board[move.x][move.y][1]) {
            board[move.x][move.y][1] = board[move.x][move.y][1] - neighbours[move.x][move.y].length;
            if (board[move.x][move.y][1] == 0) {
                //Set he cell to blank and update move lists
                board[move.x][move.y][0] = 0;
                int index;
                for (index = choices[move.player] - 1; index >= 0; index--) {
                    if (moves[move.player][index].x == move.x && moves[move.player][index].y == move.y) {
                        break;
                    }
                }
                moves[move.player][index] = moves[move.player][choices[move.player] - 1];
                choices[move.player]--;
                moves[0][choices[0]++] = ALL_MOVES[0][move.x][move.y];
            }
            explode(move.x, move.y, move.player);
        }
        return this;
    }

    private void explode(final int x, final int y, final int player) {
        for (final int neighbour : neighbours[x][y]) {
            play(ALL_MOVES[player][neighbour / BOARD_SIZE][neighbour % BOARD_SIZE]);
        }
    }

    Integer terminalValue() {
        if (((choices[1] | choices[2]) > 1) && (choices[1] == 0 || choices[2] == 0)) {
            return choices[1] == 0 ? MinMax.MIN_VALUE : MinMax.MAX_VALUE;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return Arrays.deepToString(board);
    }

    int heuristicValue(final int player) {
        final int opponent = MinMax.flip(player);
        int orbs = choices[player] - choices[opponent];
        int explosives = 0;
        for (int m = 0; m < choices[player]; m++) {
            final int i = moves[player][m].x;
            final int j = moves[player][m].y;
            if (board[i][j][1] == neighbours[i][j].length - 1) {
                explosives++;
            }
        }
        for (int m = 0; m < choices[opponent]; m++) {
            final int i = moves[opponent][m].x;
            final int j = moves[opponent][m].y;
            if (board[i][j][1] == neighbours[i][j].length - 1) {
                explosives--;
            }
        }
        return orbs + explosives;
    }

  
    private int[][][] getCopy(final int board[][][]) {
//        final int copyBoard[][][] = new int[board.length][board.length][2];
        final int copyBoard[][][] = new int[board.length][board.length][3];
        for (int k = 1; k < PLAYERS; k++) {
//        for (int k = 0; k < PLAYERS; k++) {
            for (int l = 0; l < choices[k]; l++) {
                final int i = moves[k][l].x;
                final int j = moves[k][l].y;
//                System.out.println(board[i][j].length + " " + copyBoard[i][j].length);
                System.arraycopy(board[i][j], 0, copyBoard[i][j], 0, board[i][j].length);
            }
        }
        return copyBoard;
    }

    static void setMoves() {
        for (int player = 0; player < PLAYERS; player++) {
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    ALL_MOVES[player][i][j] = new Move(i, j, player);
                }
            }
        }
    }

  
    Board getCopy() {
        return new Board(board, moves, choices);
    }
}