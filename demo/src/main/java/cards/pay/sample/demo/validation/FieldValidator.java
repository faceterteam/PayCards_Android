package cards.pay.sample.demo.validation;

public interface FieldValidator<V> {
    FieldValidationResult validate(V value);
}
