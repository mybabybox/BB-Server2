package common.thread;

import play.db.jpa.JPA;

/**
 * 
 */
public abstract class TransactionalRunnableTask implements Runnable {
    private static final play.api.Logger logger = play.api.Logger.apply(TransactionalRunnableTask.class);
    
    public abstract void execute();
    
    @Override
    public void run() {
        try {
            JPA.withTransaction(new play.libs.F.Callback0() {
                 public void invoke() {
                     execute();
                 }
             });
         } catch (Exception e) {
             logger.underlyingLogger().error("run() failed... "+e.getMessage(), e);
         }
    }
}
