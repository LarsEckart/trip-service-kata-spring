package kata;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("trip")
public record Trip(@Id Long id, String name) {

    public Trip(String name) {
        this(null, name);
    }

}
