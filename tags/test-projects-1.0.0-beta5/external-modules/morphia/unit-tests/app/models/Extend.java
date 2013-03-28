package models;

import com.google.code.morphia.annotations.Entity;
import play.modules.morphia.Model;

import java.lang.String;

@SuppressWarnings("serial")
@Entity
public class Extend extends Base {
    public String email;
}
