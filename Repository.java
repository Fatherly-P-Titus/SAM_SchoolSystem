package SchoolManager;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {
    Optional<T> findById(String id);
    List<T> findAll();
    T save(T entity);
    T update(T entity);
    void delete(T entity);
    void deleteById(String id);
    boolean existsById(String id);
    long count();
}