import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question {
    private final String text;
    private final List<String> options;
    private String correctOption;

    public Question(String text, List<String> options, String correctOption) {
        this.text = text;
        this.options = new ArrayList<>(options);
        this.correctOption = correctOption;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return new ArrayList<>(options);
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public Question copy() {
        return new Question(text, options, correctOption);
    }

    public void shuffleOptions() {
        Collections.shuffle(options);
    }

    public String getAnswerLine() {
        int idx = options.indexOf(correctOption);
        if (idx < 0) {
            idx = 0;
        }
        return options.get(idx);
    }
}