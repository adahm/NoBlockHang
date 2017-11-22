package server.model;

public class Gamestate {
    private int attempt;
    private String word;
    private int score;
    private char[] wordShown; //create a char array to easliy replace _ with correclty guessed letters.

    public Gamestate(){  //when connecting to the server just set the score to 0 and wait for a newgame to set the other variables.
        score = 0;

    }
    public void newgame(String word){
        this.word = word.toLowerCase(); // get word
        attempt = word.length();
        wordShown = new char[word.length()*2-1]; //length is *2 -1 because _ for every letter and " " for between each
        for (int i = 0;i<wordShown.length;i++){
            if(i%2== 0){
                wordShown[i] = '_';
            }
            else {
                wordShown[i]=' ';
            }

        }
    }
    public boolean tryWordGuess(String guess){ // try to guess word and uppdate the shown word if it is correct will update the word shown to client if it is correct.
        if (guess.equals(word)){
            for (int i = 0;i<word.length();i++){
                wordShown[i*2] = word.charAt(i);
            }
            return true;
        }

        else
            return false;

    }
    public boolean tryGuess(String letter) { //returns true if word is solved and false if it is not will also update the word shown to client
        char charLetter = letter.charAt(0);
        for (int i = 0; i < word.length(); i++) {
            if (charLetter == word.charAt(i))
                wordShown[i * 2] = charLetter;
        }

        for (char L : wordShown) {
            if (L == '_')
                return false;
        }
        return true;
    }

    public void addScore(){
        score++;
    }
    public void decreseScore(){
        score--;
    }
    public void decreseAttemps(){
        attempt--;
    }
    public int getAttempt() {
        return attempt;
    }
    public void setAttemptToZero(){
        attempt = 0;
    }
///add gameover
    public String getOutputString(boolean correct){
        String partWord;
        if(!correct && attempt == 0){
            return "Game over write START to try again"+ " Score: " +score;
        }
        else if(correct && attempt == 0){
            partWord = new String(wordShown); //convert char array to string
            return "Solved write START to go again \n word:"+ partWord  +" Attempts:"+ attempt+ " Score:"+ score;
        }
        else
            partWord = new String(wordShown); //convert char array to string
            return "word:"+ partWord  +" Attempts:"+ attempt+ " Score:"+ score;
    }


    //Gör char array med _ för bokstav och " " imellan
    //testa guess om hitta rätta ord och byt ut _ mot bokstaven
    //testa sen om ordet är löst
    //Gör en get String för output till användaren där allt skickas
    //Gör metoder för att updatera score
    //gör metod för att testa om ord som gisat är rätt.

}
