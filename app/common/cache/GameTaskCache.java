package common.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.GameTask;
import models.GameTask.TaskType;

/**
 * 
 */
public class GameTaskCache {
    // Permanent cache loaded up on system startup.

    private static List<GameTask> gameTasks;
    private static Map<Long, GameTask> idsMap;
    private static Map<TaskType, GameTask> taskTypesMap;

    static {
        gameTasks = GameTask.loadGameTasks();
        idsMap = new HashMap<>();
        taskTypesMap = new HashMap<>();
        for (GameTask gameTask : gameTasks) {
            idsMap.put(gameTask.id, gameTask);
            taskTypesMap.put(gameTask.taskType, gameTask);
        }
    }

    public static List<GameTask> getGameTasks() {
        return gameTasks;
    }
    
    public static GameTask getGameTask(Long id) {
        if (idsMap.containsKey(id)) {
            return idsMap.get(id);
        }
        return null;
    }
    
    public static GameTask getGameTask(TaskType taskType) {
        if (taskTypesMap.containsKey(taskType)) {
            return taskTypesMap.get(taskType);
        }
        return null;
    }
}