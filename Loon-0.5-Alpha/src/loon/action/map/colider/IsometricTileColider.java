/**
 * Copyright 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.4.1
 */
package loon.action.map.colider;

public class IsometricTileColider extends TileColider {

	public IsometricTileColider(int tileWidth, int tileHeight) {
		super(tileWidth, tileHeight);
	}

	@Override
	public boolean colideTile(Tile tile, int mx, int my, int offsetX,
			int offsetY) {

		return colideIsometric(tile, mx, my, offsetX, offsetY);
	}

	private boolean colideIsometric(Tile tile, int px, int py, int offsetX,
			int offsetY) {

		int x = px - tile.getX() - offsetX;
		int y = py - tile.getY() - offsetY;

		int my = tile.getHeight() / 2;
		int mx = tile.getWidth() / 2;

		if (y > my) {
			y = my - (y - my);
		}

		return !((x > mx + 1 + (2 * y)) || (x < mx - 1 - (2 * y)));

	}

}
