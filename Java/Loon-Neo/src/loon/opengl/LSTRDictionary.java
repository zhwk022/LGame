/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
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
 * @version 0.5
 */
package loon.opengl;

import loon.LRelease;
import loon.LSystem;
import loon.canvas.LColor;
import loon.font.LFont;
import loon.utils.ObjectMap;
import loon.utils.StringUtils;
import loon.utils.ObjectMap.Entries;
import loon.utils.ObjectMap.Entry;
import loon.utils.TArray;

public final class LSTRDictionary {

	private final int CACHE_SIZE = LSystem.DEFAULT_MAX_CACHE_SIZE * 2;

	private static LSTRDictionary instance;

	public final static LSTRDictionary make() {
		return new LSTRDictionary();
	}

	public final static LSTRDictionary get() {
		if (instance != null) {
			return instance;
		}
		synchronized (LSTRDictionary.class) {
			if (instance == null) {
				instance = make();
			}
			return instance;
		}
	}

	public void setAsyn(boolean asyn) {
		this.tmp_asyn = asyn;
	}

	public boolean isAsyn() {
		return this.tmp_asyn;
	}

	public boolean asyn() {
		return this.tmp_asyn;
	}

	private boolean tmp_asyn = true;

	private final ObjectMap<String, LFont> cacheList = new ObjectMap<String, LFont>(20);

	private final ObjectMap<String, Dict> fontList = new ObjectMap<String, Dict>(20);

	private final ObjectMap<LFont, Dict> englishFontList = new ObjectMap<LFont, Dict>(20);

	// 每次渲染图像到纹理时，同时追加一些常用非中文标记上去，以避免LSTRFont反复重构纹理
	private final static String ADDED = " 0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ:.^,!?@#$%^&*(){}[]<>\"'\\/+-~～▼【】：，。…？！";

	private final static char[] checkMessage = ADDED.toCharArray();

	public final static char split = '$';

	private Dict _lastDict;

	private String _lastMessage;

	private static StringBuffer _lazyKey;

	public static class Dict implements LRelease {

		TArray<Character> dicts;

		LSTRFont font;

		public static Dict newDict() {
			return new Dict();
		}

		public Dict() {
			dicts = new TArray<Character>(512);
		}

		public LSTRFont getSTR() {
			return font;
		}

		public boolean include(String mes) {
			final char[] chars = mes.toCharArray();
			int size = chars.length;
			for (int i = 0; i < size; i++) {
				char flag = chars[i];
				if (!dicts.contains(flag)) {
					return false;
				}
			}
			return true;
		}

		public boolean isClose() {
			return font.isClose();
		}

		@Override
		public void close() {
			if (font != null) {
				font.close();
				font = null;
			}
			if (dicts != null) {
				dicts.clear();
				dicts = null;
			}
		}

	}

	public void clearEnglishLazy() {
		synchronized (englishFontList) {
			for (Dict d : englishFontList.values()) {
				if (d != null) {
					d.close();
					d = null;
				}
			}
			englishFontList.clear();
		}
	}

	public void clearStringLazy() {
		synchronized (cacheList) {
			if (cacheList != null) {
				cacheList.clear();
			}
		}
		synchronized (fontList) {
			for (Dict d : fontList.values()) {
				if (d != null) {
					d.close();
					d = null;
				}
			}
			fontList.clear();
		}
	}

	public final boolean checkMessage(String key, String message) {
		final char[] chars = message.toCharArray();
		final char[] list = key.toCharArray();
		int size = chars.length;
		int limit = list.length;
		int idx = 0;
		for (int j = 0; j < limit; j++) {
			char name = list[j];
			for (int i = 0; i < size; i++) {
				char flag = chars[i];
				if (flag == name) {
					idx++;
				}
				if (idx >= limit) {
					return true;
				}
			}
			if (idx >= limit) {
				return true;
			}
		}
		return false;
	}

	private boolean checkEnglishString(String mes) {
		int len = mes.length();
		int count = 0;
		for (int n = 0; n < len; n++) {
			for (int i = 0, j = checkMessage.length; i < j; i++) {
				if (count >= len) {
					return true;
				}
				if (checkMessage[i] == mes.charAt(n)) {
					count++;
				}
			}
		}

		return count == len;
	}

	private StringBuffer tmpBuffer = null;

	public final Dict bind(final LFont font, final TArray<CharSequence> chars) {
		CharSequence[] buffers = new CharSequence[chars.size];
		for (int i = 0, size = buffers.length; i < size; i++) {
			buffers[i] = chars.get(i);
		}
		return bind(font, StringUtils.unificationCharSequence(buffers));
	}

	public final Dict bind(final LFont font, final String[] messages) {
		return bind(font, StringUtils.unificationStrings(messages));
	}

	public final Dict bind(final LFont font, final String mes) {
		if (StringUtils.isEmpty(mes)) {
			return new Dict();
		}
		if (mes.equals(_lastMessage) && _lastDict != null && !_lastDict.isClose()) {
			return _lastDict;
		}
		_lastMessage = mes;
		if (checkEnglishString(mes)) {
			Dict pDict = englishFontList.get(font);
			if (pDict != null && pDict.isClose()) {
				englishFontList.remove(font);
				pDict = null;
			}
			if (pDict == null) {
				pDict = Dict.newDict();
				pDict.font = new LSTRFont(font, ADDED, tmp_asyn);
				englishFontList.put(font, pDict);
			}
			return (_lastDict = pDict);
		}
		final String message = StringUtils.unificationStrings(mes + ADDED);
		if (cacheList.size > CACHE_SIZE) {
			clearStringLazy();
		}
		synchronized (fontList) {
			LFont cFont = cacheList.get(message);
			if (cFont == null) {
				for (Entries<String, LFont> it = cacheList.iterator(); it.hasNext();) {
					Entry<String, LFont> obj = it.next();
					String key = obj.key;
					if (checkMessage(key, message)) {
						cFont = obj.value;
						break;
					}
				}
			}
			String fontFlag = font.getFontName() + "_" + font.getStyle() + "_" + font.getSize();
			Dict pDict = fontList.get(fontFlag);
			if (pDict != null && pDict.isClose()) {
				fontList.remove(fontFlag);
				pDict = null;
			}
			// 判定当前font与字体和已存在的文字图片纹理，是否和缓存的font适配
			if ((cFont == null || pDict == null || (pDict != null && !pDict.include(mes)))) {
				if (pDict == null) {
					pDict = Dict.newDict();
					fontList.put(fontFlag, pDict);
				}
				synchronized (pDict) {
					cacheList.put(message, font);
					TArray<Character> charas = pDict.dicts;
					int oldSize = charas.size;
					char[] chars = message.toCharArray();
					int size = chars.length;
					for (int i = 0; i < size; i++) {
						char flag = chars[i];
						if (!charas.contains(flag)) {
							charas.add(flag);
						}
					}
					int newSize = charas.size;
					// 如果旧有大小，不等于新的纹理字符大小，重新扩展LSTRFont纹理字符
					if (oldSize != newSize) {
						if (pDict.font != null) {
							pDict.font.close();
							pDict.font = null;
						}
						if (tmpBuffer == null) {
							tmpBuffer = new StringBuffer(newSize);
						} else {
							tmpBuffer.delete(0, tmpBuffer.length());
						}
						for (int i = 0; i < newSize; i++) {
							tmpBuffer.append(charas.get(i));
						}
						// 个别浏览器纹理同步会卡出国，只能异步……
						pDict.font = new LSTRFont(font, tmpBuffer.toString(), tmp_asyn);
					}
				}
			}

			return (_lastDict = pDict);
		}
	}

	public final void drawString(LFont font, String message, float x, float y, float angle, LColor c) {
		Dict pDict = bind(font, message);
		if (pDict.font != null) {
			synchronized (pDict.font) {
				pDict.font.drawString(message, x, y, angle, c);
			}
		}
	}

	public final void drawString(LFont font, String message, float x, float y, float sx, float sy, float ax, float ay,
			float angle, LColor c) {
		Dict pDict = bind(font, message);
		if (pDict.font != null) {
			synchronized (pDict.font) {
				pDict.font.drawString(message, x, y, sx, sy, ax, ay, angle, c);
			}
		}
	}

	public final void drawString(GLEx gl, LFont font, String message, float x, float y, float angle, LColor c) {
		Dict pDict = bind(font, message);
		if (pDict.font != null) {
			synchronized (pDict.font) {
				pDict.font.drawString(gl, message, x, y, angle, c);
			}
		}
	}

	public final void drawString(GLEx gl, LFont font, String message, float x, float y, float sx, float sy, float angle,
			LColor c) {
		Dict pDict = bind(font, message);
		if (pDict.font != null) {
			synchronized (pDict.font) {
				pDict.font.drawString(gl, message, x, y, sx, sy, angle, c);
			}
		}
	}

	public final void drawString(GLEx gl, LFont font, String message, float x, float y, float sx, float sy, float ax,
			float ay, float angle, LColor c) {
		Dict pDict = bind(font, message);
		if (pDict.font != null) {
			synchronized (pDict.font) {
				pDict.font.drawString(gl, x, y, sx, sy, ax, ay, angle, message, c);
			}
		}
	}

	/**
	 * 生成特定字符串的缓存用ID
	 * 
	 * @param font
	 * @param text
	 * @return
	 */
	public final static String makeStringLazyKey(final LFont font, final String text) {
		int hashCode = 0;
		hashCode = LSystem.unite(hashCode, font.getSize());
		hashCode = LSystem.unite(hashCode, font.getStyle());
		hashCode = LSystem.unite(hashCode, font.getAscent());
		hashCode = LSystem.unite(hashCode, font.getLeading());
		hashCode = LSystem.unite(hashCode, font.getDescent());

		if (_lazyKey == null) {
			_lazyKey = new StringBuffer();
			_lazyKey.append(font.getFontName().toLowerCase());
			_lazyKey.append(hashCode);
			_lazyKey.append(split);
			_lazyKey.append(text);
		} else {
			_lazyKey.delete(0, _lazyKey.length());
			_lazyKey.append(font.getFontName().toLowerCase());
			_lazyKey.append(hashCode);
			_lazyKey.append(split);
			_lazyKey.append(text);
		}
		return _lazyKey.toString();
	}

	public final LSTRFont STRFont(LFont font) {
		if (fontList != null) {
			for (Dict d : fontList.values()) {
				if (d != null && d.font != null && d.font.getFont().equals(font)) {
					return d.font;
				}
			}
		}
		return null;
	}

	public final static String getAddedString() {
		return ADDED;
	}

	public final void dispose() {
		cacheList.clear();
		clearStringLazy();
		clearEnglishLazy();
	}

}
