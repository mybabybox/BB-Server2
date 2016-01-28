import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import models.Activity;
import models.Hashtag;
import models.Post;
import models.SecurityRole;
import models.SecurityRole.RoleType;
import models.SystemInfo;
import play.Application;
import play.GlobalSettings;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Call;
import babybox.events.handler.EventHandler;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.PlayAuthenticate.Resolver;
import com.feth.play.module.pa.exceptions.AccessDeniedException;
import com.feth.play.module.pa.exceptions.AuthException;

import common.cache.CalcServer;
import common.cache.FeaturedItemCache;
import common.hashtag.PostMarker;
import common.schedule.CommandChecker;
import common.schedule.JobScheduler;
import common.thread.ThreadLocalOverride;
import common.utils.DateTimeUtil;
import controllers.routes;

/**
 *
 */
public class Global extends GlobalSettings {
    private static final play.api.Logger logger = play.api.Logger.apply(Global.class);

    // Configurations
    private static final String STARTUP_BOOTSTRAP_PROP = "startup.data.bootstrap";
    private static final String RUN_BACKGROUND_TASKS_PROP = "run.backgroundtasks";

    /**
     * @param app
     */
    public void onStart(Application app) {

        final boolean runBackgroundTasks = Play.application().configuration().getBoolean(RUN_BACKGROUND_TASKS_PROP, false);
        if (runBackgroundTasks) {
            // schedule background jobs
            scheduleJobs();
        }
        
    	PlayAuthenticate.setResolver(new Resolver() {

			@Override
			public Call login() {
				// Your login page
				return routes.Application.login();
			}

			@Override
			public Call afterAuth() {
				// The user will be redirected to this page after authentication
				// if no original URL was saved
                return routes.Application.home();
			}

			@Override
			public Call afterLogout() {
				return routes.Application.index();
			}

			@Override
			public Call auth(final String provider) {
				// You can provide your own authentication implementation,
				// however the default should be sufficient for most cases
				return com.feth.play.module.pa.controllers.routes.Authenticate
						.authenticate(provider);
			}

			@Override
			public Call askMerge() {
				return routes.Account.askMerge();
			}

			@Override
			public Call askLink() {
				return routes.Account.askLink();
			}

			@Override
			public Call onException(final AuthException e) {
				if (e instanceof AccessDeniedException) {
					return routes.Signup
							.oAuthDenied(((AccessDeniedException) e)
									.getProviderKey());
				}

				// more custom problem handling here...
				return super.onException(e);
			}
        });

        final boolean doDataBootstrap = Play.application().configuration().getBoolean(STARTUP_BOOTSTRAP_PROP, false);

        if (doDataBootstrap) {
            logger.underlyingLogger().info("[Global.init()] Enabled");

            JPA.withTransaction(new play.libs.F.Callback0() {
                @Override
                public void invoke() throws Throwable {
                    init();
                }
            });
        } else {
            logger.underlyingLogger().info("[Global.init()] Disabled");
        }
	}

    /**
     * scheduleJobs
     */
    private void scheduleJobs() {
        
        //
        // Daily scheduled jobs
        //
        
        // schedule to purge Activity daily at 4:00am HKT
        JobScheduler.getInstance().schedule("purgeActivity", "0 00 4 ? * *",
                new Runnable() {
                    public void run() {
                        try {
                           JPA.withTransaction(new play.libs.F.Callback0() {
                                public void invoke() {
                                    logger.underlyingLogger().info("[JobScheduler] purgeActivity starts...");
                                    Activity.purgeActivity();
                                    logger.underlyingLogger().info("[JobScheduler] purgeActivity completed !");
                                }
                            });
                        } catch (Exception e) {
                            logger.underlyingLogger().error("[JobScheduler] purgeActivity failed...");
                        }
                    }
                });
        
        // schedule to purge sold posts daily at 4:30am HKT
        JobScheduler.getInstance().schedule("cleanupSoldPosts", "0 30 4 ? * *",
                new Runnable() {
                    public void run() {
                        try {
                           JPA.withTransaction(new play.libs.F.Callback0() {
                                public void invoke() {
                                    logger.underlyingLogger().info("[JobScheduler] cleanupSoldPosts starts...");
                                    CalcServer.instance().cleanupSoldPosts();
                                    logger.underlyingLogger().info("[JobScheduler] cleanupSoldPosts completed !");
                                }
                            });
                        } catch (Exception e) {
                            logger.underlyingLogger().error("[JobScheduler] cleanupSoldPosts failed...", e);
                        }
                    }
                });

        // schedule for Hash Marking daily at 5am HKT
        /*
        JobScheduler.getInstance().schedule("hashtagMarking", "0 0 5 ? * *",
                new Runnable() {
                    public void run() {
                        try {
                           JPA.withTransaction(new play.libs.F.Callback0() {
                                public void invoke() {
                                    logger.underlyingLogger().info("[JobScheduler] hashtagMarking starts...");
    
                                    try {
                                    	for (Hashtag hashtag: Hashtag.getRerunHashtags()){
    	                                	for (Post post : Post.getEligiblePostsForFeeds()) {
    	                        				if (post.soldMarked) {
    	                        					continue;
    	                        				}
    	                        				post.addHashtag(hashtag);
    	                        			 }
                                    	}	
    								} catch (Exception e2) {
    									// TODO: handle exception
    								}
                                	logger.underlyingLogger().info("[JobScheduler] hashtagMarking completed !");
                                }
                            });
                        } catch (Exception e) {
                            logger.underlyingLogger().error("[JobScheduler] hashtagMarking failed...");
                        }
                    }
                });
        */
        
        //
        // Interval scheduled jobs
        //
        
        JobScheduler.getInstance().schedule(
                "commandCheck", 
                DateTimeUtil.MINUTE_MILLIS,         // initial delay
                5 * DateTimeUtil.MINUTE_MILLIS,     // interval
                TimeUnit.MILLISECONDS,
                new Runnable() {
                    public void run() {
                        try {
                           JPA.withTransaction(new play.libs.F.Callback0() {
                                public void invoke() {
                                    CommandChecker.checkCommandFiles();
                                }
                            });
                        } catch (Exception e) {
                            logger.underlyingLogger().error("[JobScheduler] commandCheck failed...", e);
                        }
                    }
                });
        
        JobScheduler.getInstance().schedule(
                "systemInfoVersionCheck",
                1 * DateTimeUtil.MINUTE_MILLIS,     // initial delay
                DateTimeUtil.HOUR_MILLIS,           // interval
                TimeUnit.MILLISECONDS,
                new Runnable() {
                    public void run() {
                        try {
                            JPA.withTransaction(new play.libs.F.Callback0() {
                                @Override
                                public void invoke() throws Throwable {
                                    SystemInfo.checkVersionUpdated();
                                }
                            });
                        } catch (Exception e) {
                            logger.underlyingLogger().error("[JobScheduler] refreshSystemInfo failed...", e);
                        }
                    }
                });
        
        JobScheduler.getInstance().schedule(
                "refreshFeaturedItemCache", 
                2 * DateTimeUtil.MINUTE_MILLIS,     // initial delay
                DateTimeUtil.HOUR_MILLIS,           // interval
                TimeUnit.MILLISECONDS,
                new Runnable() {
                    public void run() {
                        try {
                            JPA.withTransaction(new play.libs.F.Callback0() {
                                @Override
                                public void invoke() throws Throwable {
                                    FeaturedItemCache.refresh();
                                }
                            });
                        } catch (Exception e) {
                            logger.underlyingLogger().error("[JobScheduler] refreshFeaturedItemCache failed...", e);
                        }
                    }
                });
        
        /*
        JobScheduler.getInstance().schedule(
                "postMarker", 
                3 * DateTimeUtil.MINUTE_MILLIS,     // initial delay
                5 * DateTimeUtil.MINUTE_MILLIS,     // interval
                TimeUnit.MILLISECONDS,
                new Runnable() {
                    public void run() {
                        try {
                            JPA.withTransaction(new play.libs.F.Callback0() {
                                public void invoke() {
                                    logger.underlyingLogger().info("[JobScheduler] postMark starts...");
                                    PostMarker.markPosts();
                                    logger.underlyingLogger().info("[JobScheduler] postMark completed !");
                                }
                            });
                        } catch (Exception e) {
                            logger.underlyingLogger().error("[JobScheduler] postMark failed...", e);
                        }
                    }
                });
         */
    }
    
	private void init() {
        if (SecurityRole.findRowCount() == 0L) {
            for (final RoleType roleType : Arrays.asList(SecurityRole.RoleType.values())) {
                final SecurityRole role = new SecurityRole();
                role.roleName = roleType.name();
                role.save();
            }
        }

        ThreadLocalOverride.setIsServerStartingUp(true);
        
        // data first time bootstrap
        DataBootstrap.bootstrap();
        
        // cache warm up
        CalcServer.instance().warmUpActivity();
        
        // init event handler
        EventHandler.getInstance();
        
        ThreadLocalOverride.setIsServerStartingUp(false);
	}

}