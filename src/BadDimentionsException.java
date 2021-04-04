package src;

public class BadDimentionsException extends RuntimeException {

        public BadDimentionsException(int dim) {
            super(dim + "is out of range ");

    }
}
