package loon.action.page;

import loon.Screen;

public abstract class BasePage {

	public abstract void onTransform(Screen screen, float position);

	public void resetTransform(Screen screen) {
		screen.setRotation(0);
		screen.setScaleX(1);
		screen.setScaleY(1);
		screen.setPivotX(-1f);
		screen.setPivotY(-1f);
		screen.setY(0);
		screen.setX(0);
		screen.setVisible(true);
	}

}
