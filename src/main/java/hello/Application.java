package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

@SpringBootApplication
@RestController
public class Application {

  private String prevAction;

  static class Self {
    public String href;
  }

  static class Links {
    public Self self;
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;
  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;
  }

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  @PostMapping("/**")
  public String index(@RequestBody ArenaUpdate arenaUpdate) {
    System.out.println(arenaUpdate);

    String myPlayer = getMyPlayer(arenaUpdate);

    String currentAction = getNextAction();

    return currentAction;
  }

  private String getMyPlayer(ArenaUpdate arenaUpdate) {
    return arenaUpdate._links.self.href;
  }

  private String getNextAction() {
    String[] commands = new String[]{"F", "R", "L", "T"};
    int i = new Random().nextInt(4);
    String currentAction = commands[i];

    if (prevAction != "T" && currentAction != "T") {
      return "T";
    }

    if (prevAction == "T" && currentAction == "T") {
      i = new Random().nextInt(4);
      currentAction = commands[i];
    }

    prevAction = currentAction;

    return currentAction;
  }

}

