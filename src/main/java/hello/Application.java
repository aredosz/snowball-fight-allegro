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
import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;


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

    @Override
    public String toString() {
      return new StringJoiner(", ", PlayerState.class.getSimpleName() + "[", "]")
              .add("x=" + x)
              .add("y=" + y)
              .add("direction='" + direction + "'")
              .add("wasHit=" + wasHit)
              .add("score=" + score)
              .toString();
    }
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
//    System.out.println(arenaUpdate);
//    writeCommittedStream.send(arenaUpdate.arena);

    String myPlayer = getMyPlayer(arenaUpdate);
    int width = arenaUpdate.arena.dims.get(0);
    int height = arenaUpdate.arena.dims.get(1);

    System.out.println("Arena size: width=" + width + ", height=" + height);

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

    System.out.println("My player state:");
    System.out.println(myPlayerState);

    if (myPlayerState == null) {
      System.out.println("Return T");
      return "T";
    }

    int myX = myPlayerState.x;
    int myY = myPlayerState.y;
    String myDirection = myPlayerState.direction;
    boolean iWasHit = myPlayerState.wasHit;

    if (iWasHit && canGoForward(width, height, otherPlayersMap, myX, myY, myDirection)) {
      System.out.println("I was hitted! Return F");
      return "F";
    } else if (iWasHit) {
      String hitNextAction = getNextAction();
      System.out.println("I was hitted! Return " + hitNextAction);
      return hitNextAction;
    }

    if (canThrow(width, height, otherPlayersMap, myX, myY, myDirection)) {
      System.out.println("Return T");
      return "T";
    }

    if (canGoForward(width, height, otherPlayersMap, myX, myY, myDirection)) {
      System.out.println("Return F");
      return "F";
    }

    String nextAction = getNextAction();

    System.out.println("Return " + nextAction);

    return nextAction;
  }

  private boolean canThrow(int width, int height, String[][] otherPlayersMap, int myX, int myY, String myDirection) {
    boolean canThrow = false;
    if (myDirection.equals("N")) {
      int checkY_1 = myY - 1;
      int checkY_2 = myY - 2;
      int checkY_3 = myY - 3;
      canThrow = (checkY_1 >= 0 && checkY_1 < height && "X".equals(otherPlayersMap[myX][checkY_1]))
              || (checkY_2 >= 0 && checkY_2 < height && "X".equals(otherPlayersMap[myX][checkY_2]))
              || (checkY_3 >= 0 && checkY_3 < height && "X".equals(otherPlayersMap[myX][checkY_3]));

    } else if (myDirection.equals("S")) {
      int checkY_1 = myY + 1;
      int checkY_2 = myY + 2;
      int checkY_3 = myY + 3;
      canThrow = (checkY_1 >= 0 && checkY_1 < height && "X".equals(otherPlayersMap[myX][checkY_1]))
              || (checkY_2 >= 0 && checkY_2 < height && "X".equals(otherPlayersMap[myX][checkY_2]))
              || (checkY_3 >= 0 && checkY_3 < height && "X".equals(otherPlayersMap[myX][checkY_3]));

    } else if (myDirection.equals("W")) {
      int checkX_1 = myX - 1;
      int checkX_2 = myX - 2;
      int checkX_3 = myX - 3;
      canThrow = (checkX_1 >= 0 && checkX_1 < width && "X".equals(otherPlayersMap[checkX_1][myY]))
              || (checkX_2 >= 0 && checkX_2 < width && "X".equals(otherPlayersMap[checkX_2][myY]))
              || (checkX_3 >= 0 && checkX_3 < width && "X".equals(otherPlayersMap[checkX_3][myY]));
    } else if (myDirection.equals("E")) {
      int checkX_1 = myX + 1;
      int checkX_2 = myX + 2;
      int checkX_3 = myX + 3;
      canThrow = (checkX_1 >= 0 && checkX_1 < width && "X".equals(otherPlayersMap[checkX_1][myY]))
              || (checkX_2 >= 0 && checkX_2 < width && "X".equals(otherPlayersMap[checkX_2][myY]))
              || (checkX_3 >= 0 && checkX_3 < width && "X".equals(otherPlayersMap[checkX_3][myY]));
    }
    return canThrow;
  }

  private boolean canGoForward(int width, int height, String[][] otherPlayersMap, int myX, int myY, String myDirection) {
    boolean canGoForward = false;
    if (myDirection.equals("N")) {
      canGoForward = myY - 1 >= 0 && !"X".equals(otherPlayersMap[myX][myY-1]);

    } else if (myDirection.equals("S")) {
      canGoForward = myY + 1 <= height - 1 && !"X".equals(otherPlayersMap[myX][myY+1]);

    } else if (myDirection.equals("W")) {
      canGoForward = myX - 1 >= 0 && !"X".equals(otherPlayersMap[myX-1][myY]);
    } else if (myDirection.equals("E")) {
      canGoForward = myX + 1 <= width + 1 && !"X".equals(otherPlayersMap[myX+1][myY]);
    }
    return canGoForward;
  }

  private String getMyPlayer(ArenaUpdate arenaUpdate) {
    return arenaUpdate._links.self.href;
  }

  private String getNextAction() {
    String[] commands = new String[]{"R", "L"};
    int i = new Random().nextInt(2);
    return commands[i];
  }

//  static class WriteCommittedStream {
//
//    final JsonStreamWriter jsonStreamWriter;
//
//    public WriteCommittedStream(String projectId, String datasetName, String tableName) throws IOException, Descriptors.DescriptorValidationException, InterruptedException {
//
//      try (BigQueryWriteClient client = BigQueryWriteClient.create()) {
//
//        WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build();
//        TableName parentTable = TableName.of(projectId, datasetName, tableName);
//        CreateWriteStreamRequest createWriteStreamRequest =
//                CreateWriteStreamRequest.newBuilder()
//                        .setParent(parentTable.toString())
//                        .setWriteStream(stream)
//                        .build();
//
//        WriteStream writeStream = client.createWriteStream(createWriteStreamRequest);
//
//        jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build();
//      }
//    }
//
//    public ApiFuture<AppendRowsResponse> send(Arena arena) {
//      Instant now = Instant.now();
//      JSONArray jsonArray = new JSONArray();
//
//      arena.state.forEach((url, playerState) -> {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("x", playerState.x);
//        jsonObject.put("y", playerState.y);
//        jsonObject.put("direction", playerState.direction);
//        jsonObject.put("wasHit", playerState.wasHit);
//        jsonObject.put("score", playerState.score);
//        jsonObject.put("player", url);
//        jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000);
//        jsonArray.put(jsonObject);
//      });
//
//      return jsonStreamWriter.append(jsonArray);
//    }
//
//  }
//
//  final String projectId = ServiceOptions.getDefaultProjectId();
//  final String datasetName = "snowball";
//  final String tableName = "events";
//
//  final WriteCommittedStream writeCommittedStream;
//
//  public Application() throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
//    writeCommittedStream = new WriteCommittedStream(projectId, datasetName, tableName);
//  }

}

