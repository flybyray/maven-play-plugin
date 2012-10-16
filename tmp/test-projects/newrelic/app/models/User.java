package models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import play.Logger;
import play.cache.Cache;
import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.GenericModel;
import play.db.jpa.Model;

/**
 * Użytkownik systemu
 */
@Table(name = "users")
// Oracle nie pozwala na utworzenie tabeli o nazwie user
@Entity
public class User extends Model {

    /**
     * Login
     */
    @Required
    @MaxSize(30)
    @Column(length = 30, nullable = false, unique = true)
    public String login;

    /**
     * Hasło
     */
    @Required
    @MaxSize(30)
    @Column(length = 30, nullable = false)
    public String password;

    /**
     * Powtórzone hasło do walidacji, nie jest zapisywane w bazie
     */
    @Required
    @MaxSize(30)
    @Transient
    @Column(name = "retyped_password")
    public String retypedPassword;

    /**
     * Data wygaśnięcia hasła
     */
    @Temporal(TemporalType.DATE)
    public Date expires;

    /**
     * Liczba błędnych prób autentykacji
     */
    @Column(nullable = false, name = "failed_login_count")
    public int failedLoginCount;

    /**
     * Czy konto zablokowane
     */
    @Column(nullable = false)
    public boolean locked;
}
