package va.rembot.database.dao;

import java.util.List;
import java.util.Optional;

//these are the basic CRUD, more can be added in specific DAOs
public interface Dao<T> {

    void create(T t);

    Optional<T> get(long id);

    List<T> getAll();

    void update(T t);

    void delete(T t);
}
