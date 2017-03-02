package gash.router.server;

public abstract class MessageObserver {

	   protected AdministerQueue administartorSubject;
	   public abstract void update();
	   public abstract void updateWorkSteal();
}
