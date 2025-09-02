package responses;


import java.util.List;

public class MultiResponse implements Response {
    private final List<String> parts;

    public MultiResponse(List<String> parts) {
        System.out.println("Ответ состоит из нескольких частей(" + parts.size() + ")");
        this.parts = parts;
    }

    @Override
    public String getMessage() {
        return parts.toString();
    }

    public List<String> getParts() {
        return parts;
    }
}
