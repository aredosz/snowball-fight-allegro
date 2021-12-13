package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@SpringBootApplication
@RestController
public class Application {

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
    int width = arenaUpdate.arena.dims.get(0);
    int height = arenaUpdate.arena.dims.get(1);

    String[][] otherPlayersMap = new String[width][height];
    List<String> collectedPlayers = arenaUpdate.arena.state.entrySet().stream()
            .filter(player -> !player.getKey().equals(myPlayer))
            .map(player -> {
              int playerX = player.getValue().x;
              int playerY = player.getValue().y;
              otherPlayersMap[playerX][playerY] = "X";
              return player.getKey();
            })
            .collect(toList());

    PlayerState myPlayerState = arenaUpdate.arena.state.entrySet().stream()
            .filter(player -> player.getKey().equals(myPlayer))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);

    if (myPlayerState == null) {
      return "T";
    }

    int myX = myPlayerState.x;
    int myY = myPlayerState.y;
    String myDirection = myPlayerState.direction;

    boolean canThrow = false;
    if (myDirection == "N") {
      int checkY_1 = myY - 1;
      int checkY_2 = myY - 2;
      int checkY_3 = myY - 3;
      canThrow = (checkY_1 >= 0 && checkY_1 < height && otherPlayersMap[myX][checkY_1].equals("X"))
              || (checkY_2 >= 0 && checkY_2 < height && otherPlayersMap[myX][checkY_2].equals("X"))
              || (checkY_3 >= 0 && checkY_3 < height && otherPlayersMap[myX][checkY_3].equals("X"));

    } else if (myDirection == "S") {
      int checkY_1 = myY + 1;
      int checkY_2 = myY + 2;
      int checkY_3 = myY + 3;
      canThrow = (checkY_1 >= 0 && checkY_1 < height && otherPlayersMap[myX][checkY_1].equals("X"))
              || (checkY_2 >= 0 && checkY_2 < height && otherPlayersMap[myX][checkY_2].equals("X"))
              || (checkY_3 >= 0 && checkY_3 < height && otherPlayersMap[myX][checkY_3].equals("X"));

    } else if (myDirection == "W") {
      int checkX_1 = myY - 1;
      int checkX_2 = myY - 2;
      int checkX_3 = myY - 3;
      canThrow = (checkX_1 >= 0 && checkX_1 < width && otherPlayersMap[checkX_1][myY].equals("X"))
              || (checkX_2 >= 0 && checkX_2 < width && otherPlayersMap[checkX_2][myY].equals("X"))
              || (checkX_3 >= 0 && checkX_3 < width && otherPlayersMap[checkX_3][myY].equals("X"));
    } else if (myDirection == "E") {
      int checkX_1 = myY + 1;
      int checkX_2 = myY + 2;
      int checkX_3 = myY + 3;
      canThrow = (checkX_1 >= 0 && checkX_1 < width && otherPlayersMap[checkX_1][myY].equals("X"))
              || (checkX_2 >= 0 && checkX_2 < width && otherPlayersMap[checkX_2][myY].equals("X"))
              || (checkX_3 >= 0 && checkX_3 < width && otherPlayersMap[checkX_3][myY].equals("X"));
    }

    if (canThrow) {
      return "T";
    }

    String currentAction = getNextAction();

    return currentAction;
  }

  private String getMyPlayer(ArenaUpdate arenaUpdate) {
    return arenaUpdate._links.self.href;
  }

  private String getNextAction() {
    String[] commands = new String[]{"F", "R", "L"};
    int i = new Random().nextInt(3);
    return commands[i];
  }

}

