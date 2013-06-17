/*
 * Copyright (C) 2008-2009 Google Inc.
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
 */

package jp.elektronika.AndroidSKKForZiio;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.ClipboardManager;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import static android.view.inputmethod.EditorInfo.*;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


import static jp.elektronika.AndroidSKKForZiio.InputMode.*;

/**
 * Example of writing an input method for a soft keyboard. This code is focused
 * on simplicity over completeness, so it should in no way be considered to be a
 * complete soft keyboard implementation. Its purpose is to provide a basic
 * example for how you would get started writing an input method, to be fleshed
 * out as appropriate.
 */
public class SoftKeyboard extends InputMethodService implements
KeyboardView.OnKeyboardActionListener {
	public static boolean isDebugMode;

	/**
	 * This boolean indicates the optional example code for performing processing
	 * of hard keys in addition to regular text generation from on-screen
	 * interaction. It would be used for input methods that perform language
	 * translations (such as converting text entered on a QWERTY keyboard to
	 * Chinese), but may not be used for input methods that are primarily intended
	 * to be used for on-screen text entry.
	 */
	static final boolean PROCESS_HARD_KEYS = true;

	private SoftKeyboard mSoftKeyboard;
	private LatinKeyboardView mInputView;
	private CandidateViewContainer mCandidateViewContainer;
	private CandidateView mCandidateView;
	private CompletionInfo[] mCompletions;
	private ArrayList<String> mSuggestions;

	private StringBuilder mComposing = new StringBuilder();
	private StringBuilder mKanji = new StringBuilder();
	private boolean mPredictionOn;
	private boolean mCompletionOn;
	private int mLastDisplayWidth;
	private boolean mCapsLock;
	private long mLastShiftTime;
	private long mMetaState;

	public int mChoosedIndex;

	private KeyboardSwitcher mKeyboardSwitcher;

	private LatinKeyboard mCurKeyboard;

	private AudioManager mAudioManager;
	private final float FX_VOLUME = 1.0f;
	private boolean mSilentMode;

	private String mWordSeparators;

	private InputMode mInputMode = HIRAKANA;
	private boolean isOkurigana = false;
	private String mOkurigana = null;
	private ArrayList<String> mCandidateList;


	// ���[�}������
	private HashMap<String, String> mRomajiMap = new HashMap<String, String>();
	{
		HashMap<String, String> m = mRomajiMap;
		m.put("a", "��");m.put("i", "��");m.put("u", "��");m.put("e", "��");m.put("o", "��");
		m.put("ka", "��");m.put("ki", "��");m.put("ku", "��");m.put("ke", "��");m.put("ko", "��");
		m.put("sa", "��");m.put("si", "��");m.put("su", "��");m.put("se", "��");m.put("so", "��");
		m.put("ta", "��");m.put("ti", "��");m.put("tu", "��");m.put("te", "��");m.put("to", "��");
		m.put("na", "��");m.put("ni", "��");m.put("nu", "��");m.put("ne", "��");m.put("no", "��");
		m.put("ha", "��");m.put("hi", "��");m.put("hu", "��");m.put("he", "��");m.put("ho", "��");
		m.put("ma", "��");m.put("mi", "��");m.put("mu", "��");m.put("me", "��");m.put("mo", "��");
		m.put("ya", "��");                 m.put("yu", "��");                  m.put("yo", "��");
		m.put("ra", "��");m.put("ri", "��");m.put("ru", "��");m.put("re", "��");m.put("ro", "��");
		m.put("wa", "��");m.put("wi", "����");m.put("we", "����");m.put("wo", "��");m.put("nn", "��");
		m.put("ga", "��");m.put("gi", "��");m.put("gu", "��");m.put("ge", "��");m.put("go", "��");
		m.put("za", "��");m.put("zi", "��");m.put("zu", "��");m.put("ze", "��");m.put("zo", "��");
		m.put("da", "��");m.put("di", "��");m.put("du", "��");m.put("de", "��");m.put("do", "��");
		m.put("ba", "��");m.put("bi", "��");m.put("bu", "��");m.put("be", "��");m.put("bo", "��");
		m.put("pa", "��");m.put("pi", "��");m.put("pu", "��");m.put("pe", "��");m.put("po", "��");
		m.put("va", "���J��");m.put("vi", "���J��");m.put("vu", "���J");m.put("ve", "���J��");m.put("vo", "���J��");

		m.put("xa", "��");m.put("xi", "��");m.put("xu", "��");m.put("xe", "��");m.put("xo", "��");
		m.put("xtu", "��");m.put("xke", "��");
		m.put("cha", "����");m.put("chi", "��");m.put("chu", "����");m.put("che", "����");m.put("cho", "����");

		// ---- �l�I��ŕύX
		//		m.put("fa", "�ӂ�");m.put("fi", "�ӂ�");m.put("fu", "�ӂ�");m.put("fe", "�ӂ�");m.put("fo", "�ӂ�");
		m.put("fa", "�ӂ�");m.put("fi", "�ӂ�");                  m.put("fe", "�ӂ�");m.put("fo", "�ӂ�");

		m.put("xya", "��");                m.put("xyu", "��");              m.put("xyo", "��");
		m.put("kya", "����");              m.put("kyu", "����");             m.put("kyo", "����");
		m.put("gya", "����");              m.put("gyu", "����");             m.put("gyo", "����");
		m.put("sya", "����");              m.put("syu", "����");             m.put("syo", "����");
		m.put("sha", "����");m.put("shi", "��");m.put("shu", "����");m.put("she", "����");m.put("sho", "����");
		m.put("ja",  "����");m.put("ji",  "��");m.put("ju", "����");m.put("je", "����");m.put("jo", "����");
		m.put("cha", "����");m.put("chi", "��");m.put("chu", "����");m.put("che", "����");m.put("cho", "����");
		m.put("tya", "����");              m.put("tyu", "����");m.put("tye", "����");m.put("tyo", "����");
		m.put("dha", "�ł�");m.put("dhi", "�ł�");m.put("dhu", "�ł�");m.put("dhe", "�ł�");m.put("dho", "�ł�");
		m.put("dya", "����");m.put("dyi", "����");m.put("dyu", "����");m.put("dye", "����");m.put("dyo", "����");
		m.put("nya", "�ɂ�");              m.put("nyu", "�ɂ�");             m.put("nyo", "�ɂ�");
		m.put("hya", "�Ђ�");              m.put("hyu", "�Ђ�");             m.put("hyo", "�Ђ�");
		m.put("pya", "�҂�");              m.put("pyu", "�҂�");             m.put("pyo", "�҂�");
		m.put("bya", "�т�");              m.put("byu", "�т�");             m.put("byo", "�т�");
		m.put("mya", "�݂�");              m.put("myu", "�݂�");             m.put("myo", "�݂�");
		m.put("rya", "���");              m.put("ryu", "���");m.put("rye", "�肥");m.put("ryo", "���");   

		//---- �l�I��Œǉ��������[�}���}�b�v
		m.put("jya",  "����");m.put("jyu", "����");m.put("jye", "����");m.put("jyo", "����");    
		m.put("fu", "��");
		m.put("xwi", "��");m.put("xwe", "��");
	}
	// �S�p�œ��͂���L�����X�g
	private HashMap<String, String> mZenkakuSeparatorMap = new HashMap<String, String>();
	{
		HashMap<String, String> m = mZenkakuSeparatorMap;
		m.put("-", "�[");m.put("!", "�I");m.put("?", "�H");m.put("~", "?");m.put("[", "�u");m.put("]", "�v");
	}	

	private String mRegKey = null;
	// �P��o�^������e
	private StringBuilder mRegEntry = new StringBuilder();
	private boolean isRegistering = false;

	static final String DICTIONARY = SKKDictionary.DICTIONARY;
	static final String USER_DICT = SKKDictionary.USER_DICT;
	private SKKDictionary mDict;
	private SKKUserDictionary mUserDict;
	private int mKanaKey = 0;
	private String mReceiveString = null;
	private boolean mUseVolumeButton = false;
	private boolean mUseMotion = false;
	private SensorManager mSensor;
	private float mOldPitch = 0;
	private float mOldRoll = 0;


	/**
	 * Main initialization of the input method component. Be sure to call to super
	 * class.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		isDebugMode = DeployUtil.isDebuggable(this);
		isDebugMode = false;

		mKeyboardSwitcher = new KeyboardSwitcher(this);
		mWordSeparators = getResources().getString(R.string.word_separators);
		mSoftKeyboard = this;

		// register to receive ringer mode changes for silent mode
		IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
		registerReceiver(mReceiver, filter);

		// register to receive mushroom
		filter = new IntentFilter(SKKMushroom.ACTION_BROADCAST);
		filter.addCategory(SKKMushroom.CATEGORY_BROADCAST);
		registerReceiver(mMushroomReceiver, filter);

		// register to receive DicTool
		filter = new IntentFilter(SKKDictDir.ACTION_BROADCAST);
		filter.addCategory(SKKDictDir.CATEGORY_BROADCAST);
		registerReceiver(mDicToolReceiver, filter);

		Context bc = getBaseContext();

		String kutouten = SKKPrefs.getPrefKutoutenType(bc);
		SKKUtils.dlog("kutouten type: " + kutouten);
		if (kutouten.equals("en")) {
			mZenkakuSeparatorMap.put(".", "�D");
			mZenkakuSeparatorMap.put(",", "�C");
		} else if (kutouten.equals("jp")) {
			mZenkakuSeparatorMap.put(".", "�B");
			mZenkakuSeparatorMap.put(",", "�A");
		} else if (kutouten.equals("jp_en")) {
			mZenkakuSeparatorMap.put(".", "�B");
			mZenkakuSeparatorMap.put(",", "�C");
		} else {
			mZenkakuSeparatorMap.put(".", "�D");
			mZenkakuSeparatorMap.put(",", "�C");
		}

		mKanaKey = SKKPrefs.getPrefKanaKey(bc);
		SKKUtils.dlog("Kana key code: " + mKanaKey);

		String dd = SKKPrefs.getPrefDictDir(bc);
		SKKUtils.dlog("dict dir: " + dd);

		int waitCount = 0;
		int maxWait = 9;
		while (!(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))) {
			SKKUtils.dlog("waiting mount SD ... " + waitCount);
			try{
				Thread.sleep(3000);
			} catch (InterruptedException e) {}

			waitCount++;
			if (waitCount > maxWait) {
				SKKUtils.dlog("no SD card found!");
				break;
			}
		}

		mDict = new SKKDictionary(dd + File.separator + DICTIONARY);
		mUserDict = new SKKUserDictionary(dd + File.separator + USER_DICT);

		mUseVolumeButton = SKKPrefs.getPrefVolumeButton(bc);	
	}

	@Override public void onDestroy() {
		mUserDict.commitChanges();
		unregisterReceiver(mReceiver);
		unregisterReceiver(mMushroomReceiver);
		unregisterReceiver(mDicToolReceiver);
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration conf) {
		/*
      if (!TextUtils.equals(conf.locale.toString(), mLocale)) {
          initSuggest(conf.locale.toString());
      }
		 */
		super.onConfigurationChanged(conf);
	}

	/**
	 * Called by the framework when your view for creating input needs to be
	 * generated. This will be called the first time your input method is
	 * displayed, and every time it needs to be re-created such as due to a
	 * configuration change.
	 */
	@Override
	public View onCreateInputView() {
		SKKUtils.dlog("onCreateInputView(): isFullsreenMode() = " + isFullscreenMode());
		mInputView = (LatinKeyboardView) getLayoutInflater().inflate(R.layout.input,
				null);
		mKeyboardSwitcher.setInputView(mInputView);
		mKeyboardSwitcher.makeKeyboards();
		mInputView.setOnKeyboardActionListener(this);
		mInputView.setShifted(false);
		mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT, 0);
		return mInputView;
	}

	/**
	 * Called by the framework when your view for showing candidates needs to be
	 * generated, like {@link #onCreateInputView}.
	 */
	@Override
	public View onCreateCandidatesView() {
		SKKUtils.dlog("onCreateCandidatesView(): isFullscreenMode() = " + isFullscreenMode());
		mKeyboardSwitcher.makeKeyboards();
		mCandidateViewContainer = (CandidateViewContainer) getLayoutInflater().inflate(
				R.layout.candidates, null);
		mCandidateViewContainer.initViews();
		mCandidateView = (CandidateView) mCandidateViewContainer.findViewById(R.id.candidates);
		mCandidateView.setService(this);
		return mCandidateViewContainer;
	}

	/**
	 * This is the main point where we do our initialization of the input method
	 * to begin operating on an application. At this point we have been bound to
	 * the client, and are now receiving all of the detailed information about the
	 * target of our edits.
	 */
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		SKKUtils.dlog("onStartInput()");
		super.onStartInput(attribute, restarting);

		// This method gets called without the input view being created.
		if (mInputView == null) {
			onCreateInputView();
		}

		initInputView(attribute, restarting);
	}

	private void initInputView(EditorInfo attribute, boolean restarting) {
		mKeyboardSwitcher.makeKeyboards();

		// Reset our state. We want to do this even if restarting, because
		// the underlying state of the text editor could have changed in any way.
		mComposing.setLength(0);
		mKanji.setLength(0);
		mCandidateList = null;
		isOkurigana = false;
		mCapsLock = false;
		updateCandidates();

		if (!restarting) {
			// Clear shift states.
			mMetaState = 0;
		}

		mPredictionOn = false;
		mCompletionOn = false;
		mCompletions = null;
		//    mInputMode = ALPHABET;
		changeMode(ALPHABET);

		// We are now going to initialize our state based on the type of
		// text being edited.
		SKKUtils.dlog("case = " + (attribute.inputType & EditorInfo.TYPE_MASK_CLASS));
		SKKUtils.dlog("valiation = " + (attribute.inputType & EditorInfo.TYPE_MASK_VARIATION));
		SKKUtils.dlog("autocomplete = " + (attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE));
		switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
		case TYPE_CLASS_NUMBER:
		case TYPE_CLASS_DATETIME:
			// Numbers and dates default to the symbols keyboard, with
			// no extra features.
			//mCurKeyboard = mSymbolsKeyboard;
			mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
					attribute.imeOptions);
			mKeyboardSwitcher.toggleSymbols();
			break;

		case TYPE_CLASS_PHONE:
			// Phones will also default to the symbols keyboard, though
			// often you will want to have a dedicated phone keyboard.
			//mCurKeyboard = mPhoneKeyboard;
			mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_PHONE,
					attribute.imeOptions);
			break;

		case TYPE_CLASS_TEXT:
			// This is general text editing. We will default to the
			// normal alphabetic keyboard, and assume that we should
			// be doing predictive text (showing candidates as the
			// user types).
			mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
					attribute.imeOptions);
			//      mInputMode = HIRAKANA;
			changeMode(HIRAKANA);
			mPredictionOn = true;

			// Make sure that passwords are not displayed in candidate view
			int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
			switch (variation) {
			case TYPE_TEXT_VARIATION_PASSWORD:
			case TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
				mPredictionOn = false;
				//        mInputMode = ALPHABET;
				changeMode(ALPHABET);
				break;
			case TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
				mPredictionOn = false;
				mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_EMAIL,
						attribute.imeOptions);
				//        mInputMode = ALPHABET;
				changeMode(ALPHABET);
				break;
			case TYPE_TEXT_VARIATION_PERSON_NAME:
				mPredictionOn = true;
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);
				break;
			case TYPE_TEXT_VARIATION_URI:
				mPredictionOn = true;
				mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_URL,
						attribute.imeOptions);
				//        mInputMode = ALPHABET;
				changeMode(ALPHABET);
				break;
			case TYPE_TEXT_VARIATION_SHORT_MESSAGE:
				mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_IM,
						attribute.imeOptions);
				mPredictionOn = true;
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);        
				break;
			case TYPE_TEXT_VARIATION_FILTER:
				mPredictionOn = false;
				//        mInputMode = ALPHABET;
				changeMode(ALPHABET);
				break;
			}

			if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
				mPredictionOn = true;
				mCompletionOn = isFullscreenMode();
			}

			// We also want to look at the current state of the editor
			// to decide whether our alphabetic keyboard should start out
			// shifted.
			updateShiftKeyState(attribute);
			break;

		default:
			// For all unknown input types, default to the alphabetic
			// keyboard with no special features.
			mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
					attribute.imeOptions);
			//      mInputMode = HIRAKANA;
			changeMode(HIRAKANA);
			updateShiftKeyState(attribute);
		}
		SKKUtils.dlog("onStartupInput: Result: mPredictionOn = " + mPredictionOn + " mCompletionOn = " + mCompletionOn);

		mInputView.closing();
		if (mCandidateView != null) mCandidateView.setSuggestions(null, false, false);
	}

	/**
	 * This is called when the user is done editing a field. We can use this to
	 * reset our state.
	 */
	@Override
	public void onFinishInput() {
		SKKUtils.dlog("onFinishInput()");
		super.onFinishInput();

		// Clear current composing text and candidates.
		mComposing.setLength(0);
		mKanji.setLength(0);
		mCandidateList = null;
		isOkurigana = false;
		updateCandidates();

		// We only hide the candidates window when finishing input on
		// a particular editor, to avoid popping the underlying application
		// up and down if the user is entering text into the bottom of
		// its window.
		setCandidatesViewShown(false);

		if (mInputView != null) {
			mInputView.closing();
		}
	}

	@Override
	public void onStartInputView(EditorInfo attribute, boolean restarting) {
		SKKUtils.dlog("onStartInputView()");
		super.onStartInputView(attribute, restarting);
		initInputView(attribute, restarting);
	}

	/**
	 * Deal with the editor reporting movement of its cursor.
	 */
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
				candidatesStart, candidatesEnd);

		// If the current selection in the text view changes, we should
		// clear whatever candidate text we have.
		/*
		 * if (mComposing.length() > 0 && (newSelStart != candidatesEnd || newSelEnd
		 * != candidatesEnd)) { Log.d("TEST", "delete in onUpdateSelection");
		 * mComposing.setLength(0); updateCandidates(); InputConnection ic =
		 * getCurrentInputConnection(); if (ic != null) { ic.finishComposingText();
		 * } }
		 */
	}

	/**
	 * This tells us about completions that the editor has determined based on the
	 * current text in it. We want to use this in fullscreen mode to show the
	 * completions ourself, since the editor can not be seen in that situation.
	 */
	@Override
	public void onDisplayCompletions(CompletionInfo[] completions) {
		SKKUtils.dlog("onDisplayCompletions");
		if (mCompletionOn) {
			mCompletions = completions;
			if (completions == null) {
				setSuggestions(null, false, false);
				return;
			}

			ArrayList<String> stringList = new ArrayList<String>();
			for (int i = 0; i < completions.length; i++) {
				CompletionInfo ci = completions[i];
				if (ci != null) {
					CharSequence s = ci.getText();
					if (s != null) stringList.add(s.toString());
				}
			}
			mChoosedIndex = 0;
			setSuggestions(stringList, true, true);
		}
	}

	/**
	 * This translates incoming hard key events in to edit operations on an
	 * InputConnection. It is only needed when using the PROCESS_HARD_KEYS option.
	 */
	private boolean translateKeyDown(int keyCode, KeyEvent event) {
		mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState, keyCode, event);
		int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
		mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
		if ((mMetaState & MetaKeyKeyListener.META_ALT_ON) != 0) SKKUtils.dlog("ALT is on");
		if ((mMetaState & MetaKeyKeyListener.META_SHIFT_ON) != 0) SKKUtils.dlog("SHIFT is on");
		if ((mMetaState & MetaKeyKeyListener.META_SYM_ON) != 0) SKKUtils.dlog("SYM is on");
		if ((mMetaState & MetaKeyKeyListener.META_ALT_LOCKED) != 0) SKKUtils.dlog("ALT is locked");
		if ((mMetaState & MetaKeyKeyListener.META_CAP_LOCKED) != 0) SKKUtils.dlog("SHIFT is locked");
		if ((mMetaState & MetaKeyKeyListener.META_SYM_LOCKED) != 0) SKKUtils.dlog("SYM is locked");

		InputConnection ic = getCurrentInputConnection();
		if (c == 0 || ic == null) {
			return false;
		}

		if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
			c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
		}

		/** 
		 * �L�����̌����B�p���I�����[�ł͂��肦�Ȃ��C������̂ō폜
    if (mComposing.length() > 0) {
      int last = mComposing.length() - 1;
      char accent = mComposing.charAt(last);
      int composed = KeyEvent.getDeadChar(accent, c);

      if (composed != 0) {
        c = composed;
        mComposing.setLength(last);
      }
    }
		 */

		onKey(c, null);

		return true;
	}

	/**
	 * Use this to monitor key events being delivered to the application. We get
	 * first crack at them, and can either resume them or let them continue to the
	 * app.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		SKKUtils.dlog("----BEGIN-------------------------------------------------------------------");
		SKKUtils.dlog("onKeyDown(): keyCode = " + keyCode + " mInputMode = " + mInputMode);
		SKKUtils.dlog("mComposing = " + mComposing + " mKanji = " + mKanji);

		if (mPredictionOn == false) return super.onKeyDown(keyCode, event);

		InputConnection ic = getCurrentInputConnection();

		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if(mUseVolumeButton && mInputMode != CHOOSE) {
				handleShift();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_BACK:
			// The InputMethodService already takes care of the back
			// key for us, to dismiss the input method if it is shown.
			// However, our keyboard could be showing a pop-up window
			// that back should dismiss, so we first allow it to do that.
			if (event.getRepeatCount() == 0 && mInputView != null) {
				if (mInputView.handleBack()) {
					return true;
				}
			}
			break;

		case KeyEvent.KEYCODE_DEL:
			// Special handling of the delete key: if we currently are
			// composing text for the user, we want to modify that instead
			// of let the application to the delete itself.
			handleBackspace();
			return true;

		case KeyEvent.KEYCODE_ENTER:
			SKKUtils.dlog("onKeyDown: KEYCODE_ENTER");
			switch (mInputMode) {
			case CHOOSE:
			case ENG2JP:
			case KANJI:
			case OKURIGANA:
			case REGISTER:
				onKey(0x0A, null);
				return true;
			default:
				return false;
			}

		case KeyEvent.KEYCODE_DPAD_LEFT:     
			choosePrevious(ic);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			chooseNext(ic);
			return true;
		default:
			// For all other keys, if we want to do transformations on
			// text being entered with a hard keyboard, we need to process
			// it and do the appropriate action.
			if (PROCESS_HARD_KEYS && mPredictionOn && translateKeyDown(keyCode, event)) {
				return true;
			}
		}
		SKKUtils.dlog("traslateKeyDown: can't reach onKey() : mPredictionOn = " + mPredictionOn);
		return super.onKeyDown(keyCode, event);
	}

	// Implementation of KeyboardViewListener
	// This is software key listener
	// ������Ƃ킩��Â炢���A�������[�h(mInputMode == KANJI)�Ƃ͊����ϊ����邽�߂̂Ђ炪�Ȃ���͂��郂�[�h
	// �����ɂ���Ђ炪�Ȃ����肵���Ƃ��Ƀ��[�h�͊����I�����[�h��CHOOSE�ɂȂ�
	public void onKey(int pcode, int[] keyCodes) {
		SKKUtils.dlog("onKey():: " + pcode + "(" + (char) pcode + ") mComp = "
				+ mComposing + " mKanji = " + mKanji + " im = " + mInputMode + " isFullScreen() = " + isFullscreenMode());
		EditorInfo ciei = getCurrentInputEditorInfo();
		InputConnection ic = getCurrentInputConnection();

		// �n�[�h�L�[��SHIFT��ALPHABET���ʂ̃L�[���͂Ƃ��ē��͂����
		// �\�t�g�L�[�͕K���������œ��͂���Meta�X�e�[�g�Ƃ̌������K�v�ɂȂ�B
		// ���Ɏ��O�ɑ啶���ɂ��ăL�[�̌������s����ɃV�t�g�𖳎�����ꍇ�������ɖ߂�
		if (SKKUtils.isAlphabet(pcode) && mInputView != null && mInputView.isShifted()) {
			pcode = Character.toUpperCase(pcode);
			updateShiftKeyState(ciei);
		}


		// put mushroom's result string
		/*
		 * mushroom�Ȏ��s���ʂ̔��f�B�ȑO�͂����ł���Ă��B
		 * ���݂͎�����onShowInputRequest()�ɓ���Ă݂Ă���B
		 * �������Ȃ̂ł܂��R�[�h�c���Ă�B
    if (mReceiveString != null && ic != null) {
    	commitTextSKK(ic, mReceiveString, 1);
    	mReceiveString = null;
        return;
    }
		 */

		// ����L�[�̏���
		switch (pcode) {
		case Keyboard.KEYCODE_DELETE:
			handleBackspace();
			return;
		case Keyboard.KEYCODE_SHIFT:
			handleShift();
			return;
		case LatinKeyboardView.KEYCODE_SHIFT_LONGPRESS:
			if (mCapsLock) {
				handleShift();
			} else {
				toggleCapsLock();
			}
			updateShiftKeyState(ciei);
			return;
		case LatinKeyboardView.KEYCODE_SLASH_LONGPRESS:
			if (mInputMode == ALPHABET || mInputMode == ZENKAKU) 
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);
			return;
		case Keyboard.KEYCODE_CANCEL:
			if (mInputMode == ALPHABET || mInputMode == ZENKAKU) {
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);
				commitTyped(ic);
				return;
			}
			handleClose();
			return;

			// ---- simeji mushroom
		case LatinKeyboardView.KEYCODE_MUSHROOM:
			// Show a menu or somethin'

			try {
				Intent mushroom = new Intent(this, jp.elektronika.AndroidSKKForZiio.SKKMushroom.class);
				mushroom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mushroom.putExtra(SKKMushroom.REPLACE_KEY, mComposing.toString());
				startActivity(mushroom);
			} catch (ActivityNotFoundException e) {

			}

			return;

			// ---- cursor move left 
		case LatinKeyboardView.KEYCODE_CURSOR_LEFT:
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
			return;

			// ---- cursor move right
		case LatinKeyboardView.KEYCODE_CURSOR_RIGHT:
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
			return;

			// ---- cursor move left (auto repeat)
		case LatinKeyboardView.KEYCODE_CURSOR_LEFT_LONGPRESS:
		{
			long t = SystemClock.uptimeMillis();
			ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 1));
		}
		return;

		// ---- cursor move right (auto repeat)
		case LatinKeyboardView.KEYCODE_CURSOR_RIGHT_LONGPRESS:
		{
			long t = SystemClock.uptimeMillis();
			ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 1));
		}
		return;

		case Keyboard.KEYCODE_MODE_CHANGE:
			if (mInputView != null) {
				changeKeyboardMode();
			}
			return;
		case 0x0A: // Enter Key
		switch (mInputMode) {
		case CHOOSE:
			pickSuggestionManually(mChoosedIndex);
			break;
		case ENG2JP:
			commitTextSKK(ic, mComposing, 1);
			mComposing.setLength(0);
			//        mInputMode = HIRAKANA;
			changeMode(HIRAKANA);
			updateCandidates();
			break;
		case KANJI:
			commitTextSKK(ic, mKanji.append(mComposing), 1);
			mComposing.setLength(0);
			mKanji.setLength(0);
			//        mInputMode = HIRAKANA;
			changeMode(HIRAKANA);
			onFinishInput();
			break;
		default:
			//        commitTextSKK(ic, mKanji.append(mComposing), 1);
			//        mComposing.setLength(0);
			//        mKanji.setLength(0);
			//        keyDownUp(KeyEvent.KEYCODE_ENTER);

			if (mComposing.length() == 0) {
				if (isRegistering) {
					// �P��o�^�I��
					isRegistering = false;
					if (mRegEntry.length() != 0) {
						String regstr = cutFirstSpaces(mRegEntry.toString());
						mUserDict.addEntry(mRegKey, regstr);
						mUserDict.commitChanges();
//						commitTextSKK(ic, regstr, 1);

						Toast toast = Toast.makeText(getApplicationContext(), 
								"�����ɒP�� \"" + regstr + 
								"\"" + " ��o�^���܂����B", Toast.LENGTH_SHORT );
						toast.show();

					}
					mRegKey = null;
					mRegEntry.setLength(0);
					reset();
				} else {
					keyDownUp(KeyEvent.KEYCODE_ENTER);
				}
			} else {
				commitTextSKK(ic, mKanji.append(mComposing), 1);
				mComposing.setLength(0);
				mKanji.setLength(0);
				onFinishInput();
				keyDownUp(KeyEvent.KEYCODE_ENTER);
			}
			break;
		}
		return;
		case 'l':
			if (mInputMode == HIRAKANA || mInputMode == KATAKANA) {
				commitTyped(ic);
				//        mInputMode = ALPHABET;
				changeMode(ALPHABET);
				return;
			}
			break;
		case 'L':
			if (mInputMode != ALPHABET && mInputMode != ZENKAKU && mInputMode != ENG2JP) {
				commitTyped(ic);
				//        mInputMode = ZENKAKU;
				changeMode(ZENKAKU);
				return;
			}
			break;
		case 'q':
			if (mInputMode == ALPHABET || mInputMode == ENG2JP || mInputMode == ZENKAKU)
				break;

			switch (mInputMode) {
			case HIRAKANA:
				//        mInputMode = KATAKANA;
				if (!isRegistering) changeMode(KATAKANA);
				break;
			case KATAKANA:
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);
				break;
			case KANJI:
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);
				if (mKanji.length() > 0) {
					String str = SKKUtils.hirakana2katakana(mKanji.toString());
					commitTextSKK(ic, str, 1);
					mKanji.setLength(0);
					onFinishInput();
				}
				break;
			}

			if (mComposing.length() > 0) commitTyped(ic);
			return;
		case '/':
			if (mInputMode == HIRAKANA || mInputMode == KATAKANA) {
				//        mInputMode = ENG2JP;
				changeMode(ENG2JP);
				return;
			}
			break;
		}

		// ALPHABET�Ȃ�commit���ďI��
		if (mInputMode == ALPHABET) {
			commitTextSKK(ic, String.valueOf((char) pcode), 1);
			onFinishInput();
			return;
		}

		// Zenkaku�Ȃ�S�p�ϊ���commit���ďI��
		if (mInputMode == ZENKAKU) {
			pcode = SKKUtils.hankaku2zenkaku(pcode);
			commitTextSKK(ic, String.valueOf((char) pcode), 1);
			onFinishInput();
			return;
		}


		// �p���ϊ��Ȃ��؂蕶���Ŋm�肷�邩���̂܂�Composing�ɐς�
		if (mInputMode == ENG2JP) {
			if (isWordSeparator(pcode)) {
				handleSeparator(pcode, mComposing);
				return;
			}

			handleEnglish(pcode, keyCodes);
			return;
		}

		if (mInputMode == CHOOSE) {
			switch (pcode) {
			case ' ':
				//        chooseNext(ic);
				//        return;
				if (mChoosedIndex == mSuggestions.size() - 1) {
					if (!isRegistering) registerStart(ic, mKanji.toString());
				} else {
					chooseNext(ic);
				}
				return;
			case 'x':
				choosePrevious(ic);
				if (mChoosedIndex == mSuggestions.size() - 1) {
					if (mKanji.length() != 0) {
						// Back to Kanji
						if (isOkurigana) mKanji.append(mOkurigana);
						setComposingTextSKK(ic, mKanji, 1);
						//            mInputMode = KANJI;
						changeMode(KANJI);
					} else {
						setComposingTextSKK(ic, mComposing, 1);
						//            mInputMode = ENG2JP;
						changeMode(ENG2JP);
					}
					setSuggestions(null, false, false);
				}
				return;
			case Keyboard.KEYCODE_DELETE:
				handleBackspace();
				updateCandidates();
				//mInputMode = (mKanji.length() > 0) ? HIRAKANA : ENG2JP;
				return;
			case 'c':
				mCandidateView.scrollPrev();
				mChoosedIndex = mCandidateView.getScrolledFirstIndex();
				mCandidateView.choose(mChoosedIndex);
				{
					String cad = SKKUtils.removeAnnotation(mSuggestions.get(mChoosedIndex));
					if (isOkurigana) cad = cad.concat(mOkurigana);
					setComposingTextSKK(ic, cad, 1);
				}
				return;
			case 'v':
				mCandidateView.scrollNext();
				mChoosedIndex = mCandidateView.getScrolledFirstIndex();
				mCandidateView.choose(mChoosedIndex);
				{
					String cad = SKKUtils.removeAnnotation(mSuggestions.get(mChoosedIndex));
					if (isOkurigana) cad = cad.concat(mOkurigana);
					setComposingTextSKK(ic, cad, 1);
				}
				return;
			default:
				pickSuggestionManually(mChoosedIndex);
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);
				onKey(pcode, null);
				return;
			}
		}

		// �������[�h�ŋ�؂蕶���̏ꍇ�A�ϊ��J�n
		if (isWordSeparator(pcode) && mInputMode == KANJI) {
			// �Ō�ɒP�̂�'n'�ŏI����Ă���ꍇ�A'��'�ɕϊ�
			if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
				mKanji.append('��');
				setComposingTextSKK(ic, mKanji, 1);
			}

			handleSeparator(pcode, mKanji);
			return;
		}

		// �V�t�g�L�[�̏���
		boolean isUpper = Character.isUpperCase(pcode);
		if (isUpper) { // ���[�}���ϊ��̂��߂ɏ������ɖ߂�
			pcode = Character.toLowerCase(pcode);
		}
		// �V�t�g�L�[�𗣂��̂��ʓ|�Ȃ̂�OKURIGANA���莞�ɑ啶���̎��ɂ̓V�t�g����
		if (mInputMode == OKURIGANA && isUpper) {
			isUpper = false;
		}

		// �����ł͊��Ɋ����ϊ����[�h�ł��邩�������Љ����̓��͂ł���̂ňꕔ�̋L���͑S�p�ɂ���
		pcode = changeSeparator2Zenkaku(pcode);

		// '��'��'��'�̏���
		if (mComposing.length() == 1) {
			char first = mComposing.charAt(0);
			if (first == 'n') {
				if (!SKKUtils.isVowel(pcode) && pcode != 'n' && pcode != 'y') {
					String str = "��";
					handleNN(ic, str);
				}
			} else if (first == pcode) {
				String str = "��";
				handleNN(ic, str);
			}
		}

		if ((mInputView != null && mInputView.isShifted()) || isUpper) {
			// �V�t�g�L�[��������Ă����ԁF
			// �������[�h�Ȃ瑗�艼���J�n�ł���ϊ����s�Ȃ��B�����̃L�[�͊����ǂ�+���艼���A���t�@�x�b�g1����
			// �ŏ��̕������͂��V�t�g�L�[���������ςȂ��ɂ��Ă��܂����߁AmKanji�̒������`�F�b�N
			// mKanji�̒�����0�̎��̓V�t�g��������Ă��Ȃ��������Ƃɂ��ĉ����֌p��������
			if (mKanji.length() > 0 && mInputMode == KANJI) {
				mKanji.append((char) pcode); //���������ɂ͑��艼���̎q���������K�v
				Log.d("henkan", "henkan");
				setComposingTextSKK(ic, mKanji, 1);
				ArrayList<String> cand = findKanji(mKanji.toString());
				// dictionary
				if (cand != null) {
					isOkurigana = true;
					mChoosedIndex = 0;

					mComposing.setLength(0);
					mComposing.append((char) pcode);
					mKanji.deleteCharAt(mKanji.length() - 1); // ���艼���̎q����������菜��

					// �u�����������v�Ȃ瑦���艼������
					if (SKKUtils.isVowel(pcode)) {
						String str = changeAlphabet2Romaji();

						mKanji.append(str);
						mOkurigana = str;
						//            mInputMode = CHOOSE;
						changeMode(CHOOSE);
						setSuggestions(cand, true, true);
						setComposingTextSKK(ic, SKKUtils.removeAnnotation(cand.get(0)).concat(str), 1); // �ϊ����̍ŏ���EditorView�ɕ\��
					} else { // ����ȊO�͑��艼�����[�h
						//            mInputMode = OKURIGANA;
						changeMode(OKURIGANA);
						mCandidateList = cand;
						updateCandidates();
					}
				} else {
					// �ϊ����s�A�����o�^
					registerStart(ic, mKanji.toString());
					/*
          setComposingTextSKK(ic, mKanji, 1);
          mComposing.append((char) pcode);
          mKanji.deleteCharAt(mKanji.length() - 1); // ���艼���̎q����������菜��
          mSuggestions.clear();
          mSuggestions.add("IME�F���o�^");
          setSuggestions(mSuggestions, false, false);
					 */
				}

				return;
			} else if (mInputMode == HIRAKANA) {
				// �������Ȃ犿���ϊ������͂̊J�n�BKANJI�ւ̈ڍs

				// ���[�}���̓r���Ŋ����ϊ��ɓ������ꍇ�A�r���܂ł̃A���t�@�x�b�g��|���o��
				if (mComposing.length() > 0) {
					commitTextSKK(ic, mComposing, 1);
					mComposing.setLength(0);
					onFinishInput();
				}
				//        mInputMode = KANJI;
				changeMode(KANJI);
				// �����ł͕\���̂ݏC�������֔���������
				setComposingTextSKK(ic, mComposing, 1);
				updateCandidates();
			}
		}


		String hchr; // �Ђ炪�ȁA1���[�}���P�ʕ��A"���A���A���A�A����A���A����A�A"
		if (pcode == '�[') {
			hchr = "�[";
		} else {
			mComposing.append((char) pcode);
			hchr = changeAlphabet2Romaji(); // ���[�}������Ђ炪�Ȃɕϊ�
		}
		if (hchr != null) {
			// Success
			if (mInputMode == KATAKANA) {
				hchr = SKKUtils.hirakana2katakana(hchr);
			}

			mComposing.setLength(0);

			if (mInputMode == KANJI) {
				mKanji.append(hchr);
				setComposingTextSKK(ic, mKanji, 1);
			} else if (mInputMode == OKURIGANA) {
				setSuggestions(mCandidateList, false, true);
				//        mInputMode = CHOOSE;
				changeMode(CHOOSE);
				mOkurigana = (mOkurigana == null) ? hchr : mOkurigana.concat(hchr);
				if (mCandidateList != null)
					setComposingTextSKK(ic, SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex).concat(mOkurigana)), 1);
				return;
			} else {
				commitTextSKK(ic, hchr, 1);
				onFinishInput();
			}

			// sendKey(pcode);
			updateCandidates();
			return;
		}

		// �\�����ďI��:
		// �����ɗ����Ȃ�mInputMode�Ɍ��炸���m��
		switch (mInputMode) {
		case HIRAKANA:
		case KATAKANA:
			if (SKKUtils.isAlphabet(pcode)) {
				setComposingTextSKK(ic, mComposing, 1);
			} else {
				commitTextSKK(ic, mComposing, 1);
				mComposing.setLength(0);
				onFinishInput();
			}

			updateCandidates();
			break;
		case KANJI:
		case OKURIGANA:
			String str = "" + mKanji + mComposing;
			setComposingTextSKK(ic, str, 1);
			updateCandidates();
			break;
		default:
			commitTextSKK(ic, mComposing, 1);
			mComposing.setLength(0);
			onFinishInput();
			break;
		}

		SKKUtils.dlog("End: mComposing = " + mComposing + " mKanji = " + mKanji + " mInputMode = " + mInputMode);
		SKKUtils.dlog("--------------------------------------------------------------------------------");
	}

	private void changeKeyboardMode() {
		mKeyboardSwitcher.toggleSymbols();
		if (mCapsLock && mKeyboardSwitcher.isAlphabetMode()) {
			((LatinKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
		}

		updateShiftKeyState(getCurrentInputEditorInfo());
	}

	private void choosePrevious(InputConnection ic) {
		if (mSuggestions == null) return;
		String cad;
		mChoosedIndex--;
		if (mChoosedIndex < 0) mChoosedIndex = mSuggestions.size() - 1;
		mCandidateView.choose(mChoosedIndex);
		cad = SKKUtils.removeAnnotation(mSuggestions.get(mChoosedIndex));
		if (isOkurigana) cad = cad.concat(mOkurigana);
		setComposingTextSKK(ic, cad, 1);
		return;
	}

	private void chooseNext(InputConnection ic) {
		if (mSuggestions == null) return;
		mChoosedIndex++;
		if (mChoosedIndex >= mSuggestions.size()) mChoosedIndex = 0;
		mCandidateView.choose(mChoosedIndex);
		String cad = SKKUtils.removeAnnotation(mSuggestions.get(mChoosedIndex));
		if (isOkurigana) cad = cad.concat(mOkurigana);
		setComposingTextSKK(ic, cad, 1);
		return;
	}

	private void handleSeparator(int pcode, StringBuilder composing) {
		EditorInfo ciei = getCurrentInputEditorInfo();
		InputConnection ic = getCurrentInputConnection();
		String str = composing.toString();
		if (str.length() > 0) {
			ArrayList<String> list = findKanji(str);
			if (list == null)
				return; // FUTURE: REGISTER

				mChoosedIndex = 0;
			//      mInputMode = CHOOSE;
				changeMode(CHOOSE);
				setComposingTextSKK(ic, list.get(0), 1);
				setSuggestions(list, false, true);

				return;
		}

		// Handle separator
		mComposing.append((char) pcode);
		commitTyped(ic);
		// sendKey(pcode);
		updateShiftKeyState(ciei);
		return;
	}


	// �Ђ炪�Ȃł͈ȉ��̕��������S�p�ɂȂ�B�����̎�Ō��肵�Ă܂��B�K���ɏC�����Ă��������B
	private int changeSeparator2Zenkaku(int pcode) {
		char c;
		switch (pcode) {
		case '.':
			c = '�B';
			break;
		case ',':
			c = '�A';
			break;
		case '-':
			c = '�[';
			break;
		case '!':
			c = '�I';
			break;
		case '?':
			c = '�H';
			break;
		case '~':
			c = '�`';
			break;
		default:
			c = (char) pcode;
		}
		return (int) c;
	}

	// "��"��"��"����舵��
	// KANJI�Ȃ�mKanji�ɂ������A�o�͂�ς���
	private void handleNN(InputConnection ic, String str) {
		if (mInputMode == KATAKANA) str = SKKUtils.hirakana2katakana(str);

		if (mInputMode == KANJI) {
			mKanji.append(str);
			setComposingTextSKK(ic, mKanji, 1);
		} else if (mInputMode == OKURIGANA) {
			mOkurigana = str;
			mKanji.append(str);
			setComposingTextSKK(ic, mKanji, 1);
		} else { // HIRAGANA, KATAKANA
			commitTextSKK(ic, str, 1);
		}
		mComposing.setLength(0);
	}

	/**
	 * Use this to monitor key events being delivered to the application. We get
	 * first crack at them, and can either resume them or let them continue to the
	 * app.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// If we want to do transformations on text being entered with a hard
		// keyboard, we need to process the up events to update the meta key
		// state we are tracking.
		if (PROCESS_HARD_KEYS) {
			if (mPredictionOn) {
				mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState, keyCode, event);
			}
		}

		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Helper function to commit any text being composed in to the editor.
	 */
	private void commitTyped(InputConnection inputConnection) {
		if (mComposing.length() > 0) {
			commitTextSKK(inputConnection, mComposing, 1);
			mComposing.setLength(0);
			updateCandidates();
		}
	}

	/**
	 * Helper to update the shift state of our keyboard based on the initial
	 * editor state.
	 */
	private void updateShiftKeyState(EditorInfo attr) {
		InputConnection ic = getCurrentInputConnection();
		if (attr != null && mInputView != null && mKeyboardSwitcher.isAlphabetMode()
				&& ic != null) {
			int caps = 0;
			EditorInfo ei = getCurrentInputEditorInfo();
			if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
				caps = ic.getCursorCapsMode(attr.inputType);
			}
			// �J�[�\���ʒu�Ŏ����V�t�g�I���̋@�\�͂������̂ł��
			// mInputView.setShifted(mCapsLock || caps != 0);
			mInputView.setShifted(mCapsLock);
		}
	}


	/**
	 * Helper to send a key down / key up pair to the current editor.
	 */
	private void keyDownUp(int keyEventCode) {
		InputConnection ic = getCurrentInputConnection();
		ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}

	/**
	 * Helper to send a character to the editor as raw key events.
	 */
	private void sendKey(int keyCode) {
		switch (keyCode) {
		case '\n':
			keyDownUp(KeyEvent.KEYCODE_ENTER);
			break;
		default:
			if (keyCode >= '0' && keyCode <= '9') {
				keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
			} else {
				InputConnection ic = getCurrentInputConnection();
				commitTextSKK(ic, String.valueOf((char) keyCode),
						1);
			}
			break;
		}
	}

	// Implementation of KeyboardViewListener

	private ArrayList<String> findKanji(String key) {
		SKKUtils.dlog("findKanji(): key=" + key);
		ArrayList<String> list1 = mDict.getCandidates(key);
		ArrayList<String> list2 = mUserDict.getCandidates(key);

		if (list1 == null && list2 == null) {
			SKKUtils.dlog("Dictoinary: Can't find Kanji for " + key);
			return null;
		}

		if (list1 == null) list1 = new ArrayList<String>();
		if (list2 != null) {
			int idx = 0;
			for (String s : list2) {
				//�l�����̌���擪�ɒǉ�
				list1.remove(s);
				list1.add(idx, s);
				idx++;
			}
		}

		return list1;
	}

	private String changeAlphabet2Romaji() {
		String result = mRomajiMap.get(mComposing.toString());
		return result;
	}


	public void onText(CharSequence text) {
		InputConnection ic = getCurrentInputConnection();
		if (ic == null)
			return;
		ic.beginBatchEdit();
		if (mComposing.length() > 0) {
			commitTyped(ic);
		}
		commitTextSKK(ic, text, 0);
		ic.endBatchEdit();
		updateShiftKeyState(getCurrentInputEditorInfo());
	}

	/**
	 * Update the list of available candidates from the current composing text.
	 * This will need to be filled in by however you are determining candidates.
	 */
	private void updateCandidates() {
		mChoosedIndex = 0;
		int clen = mComposing.length();
		int klen = mKanji.length();

		if (clen == 0 && klen == 0) {
			setSuggestions(null, false, true);
			return;
		}

		String str = mComposing.toString();
		ArrayList<String> list = new ArrayList<String>();

		switch (mInputMode) {
		case HIRAKANA:
		case KATAKANA:
		case OKURIGANA:
			list.add(str);
			break;
		case ENG2JP:
			list.add(str);
			mDict.findKeys(str, false, list);
			break;
		case KANJI:
			if (clen == 0) {
				str = mKanji.toString();
				list.add(str);
			} else {
				list.add(str);
				String tmp = str.concat("a"); // ���[�}�����͒��͂Ƃ肠�����A�s�Ɏ؂茈�߂��Č����B�������Ȃ��Ɖp�P�ꂪ�o�Ďg���Ȃ�
				tmp = mRomajiMap.get(tmp);
				if (tmp != null) str = tmp;
				str = mKanji.toString().concat(str);
			}

			mDict.findKeys(str, true, list);
			break;
		default:
			SKKUtils.dlog("updateCandidates(): Unknown case: " + mInputMode);
		}

		setSuggestions(list, false, false);
	}

	public void setSuggestions(ArrayList<String> suggestions,
			boolean completions, boolean typedWordValid) {
		if (suggestions != null && suggestions.size() > 0) {
			mSuggestions = suggestions;
			setCandidatesViewShown(true);
		} else if (isExtractViewShown()) {
			setCandidatesViewShown(true);
		}
		if (mCandidateView != null) {
			mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
		}
	}

	private void handleBackspace() {
		int clen = mComposing.length();
		int klen = mKanji.length();
		SKKUtils.dlog("handleBackspace(): clen = " + clen + " klen = " + klen + " mInp = " + mInputMode + " mComp = " + mComposing + " mKan = " + mKanji);

		InputConnection ic = getCurrentInputConnection();
		if (clen > 1) {
			mComposing.delete(clen - 1, clen);
			setComposingTextSKK(ic, mComposing, 1);

		} else if (clen == 1) {
			SKKUtils.dlog("delete in handleBackspace()");
			mComposing.setLength(0);
			if (klen > 0) {
				setComposingTextSKK(ic, mKanji, 1);
			} else
				commitTextSKK(ic, "", 0);

		} else { // length == 0
			if (klen > 0) mKanji.delete(klen - 1, klen);
			setComposingTextSKK(ic, mKanji, 1);
			if (klen == 0) keyDownUp(KeyEvent.KEYCODE_DEL);
		}
		if (mSuggestions != null) mSuggestions.clear();
		updateCandidates();
		// �폜��̒����ōX�V
		clen = mComposing.length();
		klen = mKanji.length();
		switch (mInputMode) {
		case CHOOSE:
			if (klen == 0) {
				changeMode((clen > 0) ? ENG2JP : HIRAKANA);
			} else {
				//        mInputMode = KANJI;
				changeMode(KANJI);
			}
			isOkurigana = false;
			mOkurigana = null;
			break;
		case OKURIGANA:
			if (clen == 0) {
				isOkurigana = false;
				mOkurigana = null;
				//        mInputMode = KANJI;
				changeMode(KANJI);
			}
			break;
		case KANJI:
			//      if (klen == 0 && clen == 0) mInputMode = HIRAKANA;
			if (klen == 0 && clen == 0) changeMode(HIRAKANA);
			break;
		case ENG2JP:
			//      if (clen == 0) mInputMode = HIRAKANA;
			if (clen == 0) changeMode(HIRAKANA);
			break;
		}
		if (isRegistering) {
			if (klen == 0 && clen == 0) {
				isRegistering = false;
				mRegKey = null;
				mRegEntry.setLength(0);
				reset();
			}
		}
	}

	private void handleShift() {
		if (mInputView == null) {
			return;
		}

		Keyboard currentKeyboard = mInputView.getKeyboard();
		if (mKeyboardSwitcher.isAlphabetMode()) {
			// Alphabet keyboard
			checkToggleCapsLock();
			mInputView.setShifted(mCapsLock || !mInputView.isShifted());
		} else {
			mKeyboardSwitcher.toggleShift();
		}
	}

	private void handleEnglish(int prime, int[] keyCodes) {
		SKKUtils.dlog("handleEnglish()");
		InputConnection ic = getCurrentInputConnection();
		mComposing.append((char) prime);
		setComposingTextSKK(ic, mComposing, 1);
		updateShiftKeyState(getCurrentInputEditorInfo());
		updateCandidates();
	}


	private void handleClose() {
		commitTyped(getCurrentInputConnection());
		requestHideSelf(0);
		mInputView.closing();
	}

	private void checkToggleCapsLock() {
		if (mInputView.getKeyboard().isShifted()) {
			toggleCapsLock();
		}
	}

	public boolean isWordSeparator(int code) {
		return mWordSeparators.contains(String.valueOf((char) code));
	}

	public void pickDefaultCandidate() {
		pickSuggestionManually(0);
	}

	public void pickSuggestionManually(int index) {
		SKKUtils.dlog("pickSuggestionManually: mCompletionOn = " + mCompletionOn);
		InputConnection ic = getCurrentInputConnection();
		if (mCompletionOn && mCompletions != null && index >= 0 && index < mCompletions.length) {
			CompletionInfo ci = mCompletions[index];
			ic.commitCompletion(ci);
			if (mCandidateView != null) {
				mCandidateView.clear();
			}
			updateShiftKeyState(getCurrentInputEditorInfo());
		} else if (mSuggestions.size() > 0) {
			// If we were generating candidate suggestions for the current
			// text, we would commit one of them here. But for this sample,
			// we will just commit the current text.
			String s = mSuggestions.get(index);

			switch (mInputMode) {
			case CHOOSE:
				commitTextSKK(ic, SKKUtils.removeAnnotation(s), 1);
				if (isOkurigana) commitTextSKK(ic, mOkurigana, 1);

				mComposing.setLength(0);
				mKanji.setLength(0);
				//        mInputMode = HIRAKANA;
				changeMode(HIRAKANA);
				isOkurigana = false;
				mOkurigana = null;
				updateCandidates();
				onFinishInput();
				break;
			case ENG2JP:
				setComposingTextSKK(ic, s, 1);
				mComposing.setLength(0);
				mComposing.append(s);
				ArrayList<String> list = findKanji(s);
				setSuggestions(list, false, true);
				//        mInputMode = CHOOSE;
				changeMode(CHOOSE);
				break;
			case KANJI:
				setComposingTextSKK(ic, s, 1);
				int li = s.length() - 1;
				int last = s.codePointAt(li);
				if (SKKUtils.isAlphabet(last)) {
					mKanji.setLength(0);
					mKanji.append(s.substring(0, li));
					mComposing.setLength(0);
					onKey(Character.toUpperCase(last), null); 
				} else {
					mKanji.setLength(0);
					mKanji.append(s);
					mComposing.setLength(0);
					list = findKanji(s);
					setSuggestions(list, false, true);
					//          mInputMode = CHOOSE;
					changeMode(CHOOSE);
				}
				break;
			}
		}
	}

	private void toggleCapsLock() {
		mCapsLock = !mCapsLock;
		if (mKeyboardSwitcher.isAlphabetMode()) {
			((LatinKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
		}
	}

	public void swipeRight() {
		if (mCompletionOn) {
			pickDefaultCandidate();
		}
	}

	public void swipeLeft() {
		handleBackspace();
	}

	public void swipeDown() {
		handleClose();
	}

	public void swipeUp() {
	}

	public void onPress(int primaryCode) {
	}

	public void onRelease(int primaryCode) {
	}

	// receive ringer mode changes to detect silent mode
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateRingerMode();
		}
	};

	// receive from mushroom
	private BroadcastReceiver mMushroomReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			String s = extras.getString(SKKMushroom.REPLACE_KEY);

			if (mInputView == null) return;

			/*
    	  InputConnection ic = getCurrentInputConnection();
    	  if (ic != null) {
    		  if (ic.commitText(s, 1) == false) {
        		  Toast toast = Toast.makeText(getApplicationContext(), "false!", Toast.LENGTH_SHORT );
        		  toast.show();
    		  }
    	  }
			 */

			if (mReceiveString == null) {
				mReceiveString = new String(s);
			}

			if (s != null) {              
				ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				cm.setText(s);
				Toast toast = Toast.makeText(getApplicationContext(), "Note: ���s���ʂ̓N���b�v�{�[�h�ɂ������Ă��܂��B", Toast.LENGTH_SHORT );
				toast.show();
			}
		}
	};


	// receive from SKK Preference
	private BroadcastReceiver mDicToolReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			String s = extras.getString(SKKDictDir.VALUE_KEY);
			if (s != null) {
				// ---- reopen DICTIONARIES
				String dd = SKKPrefs.getPrefDictDir(getBaseContext());
				mUserDict = new SKKUserDictionary(dd + File.separator + USER_DICT);
				mDict = new SKKUserDictionary(dd + File.separator + DICTIONARY);
			}
		}
	};

	
	// update flags for silent mode
	private void updateRingerMode() {
		if (mAudioManager == null) {
			mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		}
		if (mAudioManager != null) {
			mSilentMode = (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
		}
	}

	@Override
	public void onBindInput() {
		SKKUtils.dlog("onBindInput()");
		super.onBindInput();
	}

	// Disable Fullscreen mode
	@Override
	public boolean onEvaluateFullscreenMode () {
		super.onEvaluateFullscreenMode();
		return false;
	}

	// change the mode and set the status icon
	private void changeMode(InputMode im) {
		int icon = 0;

		switch (im) {
		case HIRAKANA:
			mInputMode = HIRAKANA;
			icon = R.drawable.immodeic_hiragana;
			//setCandidatesViewShown()�ł�ComposingText��flush�������ۂ�
			//�P��o�^�����ƕςɂȂ�̂Œ���
			//	  setCandidatesViewShown(false);
			break;
		case KATAKANA:
			mInputMode = KATAKANA;
			icon = R.drawable.immodeic_katakana;
			//	  setCandidatesViewShown(false);
			break;
		case KANJI:
			mInputMode = KANJI;
			break;
		case CHOOSE:
			mInputMode = CHOOSE;
			break;
		case ZENKAKU:
			mInputMode = ZENKAKU;
			icon = R.drawable.immodeic_full_alphabet;
			//	  setCandidatesViewShown(false);
			break;
		case ENG2JP:
			mInputMode = ENG2JP;
			icon = R.drawable.immodeic_eng2jp;
			break;
		case OKURIGANA:
			mInputMode = OKURIGANA;
			break;
		default:
			icon = 0;
			mInputMode = im;
			showStatusIcon(icon);
			break;
		}

		if (icon != 0) showStatusIcon(icon);
	}



	// commitText�̃��b�p�[ �o�^��ƒ��Ȃ�o�^���e�ɒǉ����C�\�����X�V
	private void commitTextSKK(InputConnection ic, CharSequence text, int newCursorPosition) {
		if (isRegistering) {
			mRegEntry.append(text);
			ic.setComposingText(text, newCursorPosition);
		} else {
			ic.commitText(text, newCursorPosition);
		}
	}

	//setComposingText�̃��b�p�[
	private void setComposingTextSKK(InputConnection ic, CharSequence text, int newCursorPosition) {
		StringBuilder ct = new StringBuilder();

		if (isRegistering) {
			ct.append("��");
			ct.append(mRegKey);
			ct.append("�F");
			ct.append(mRegEntry);
		}

		if (!text.equals("")) {
			switch (mInputMode) {
			case KANJI:
			case ENG2JP:
			case OKURIGANA:
				ct.append("��");
				break;
			case CHOOSE:
				ct.append("��");
				break;
			default:
				break;
			}
		} 
		ct.append(text);
		ic.setComposingText(ct, newCursorPosition);
	}

	private void reset() {
		mComposing.setLength(0);
		mKanji.setLength(0);
		mOkurigana = null;
		mCandidateList = null;
		setSuggestions(null, false, false);
		getCurrentInputConnection().setComposingText("", 1);
	}

	private void registerStart(InputConnection ic, String str) {
		mRegKey = str;
		mRegEntry.setLength(0);
		changeMode(HIRAKANA);
		reset();
		Toast toast = Toast.makeText(getApplicationContext(), "����ȏ�̒P�ꂪ����܂���B\n" +
				"�����o�^���J�n���܂��B", Toast.LENGTH_SHORT );
		toast.show();
		setComposingTextSKK(ic, "�����o�^�J�n", 1);
		isRegistering = true;
	}

	// ---- ������B����ǂ����Ő擪�ɃX�y�[�X������������Ă�̂ō���Ă�B
	private String cutFirstSpaces(String s) {
		String ret = s.replaceFirst(" +", "");
		return ret;
	}

	// ---- mushroom�Ȏ��s���ʂ̔��f�B���߂��ɃR�R�֓���Ă݂��B
	@Override
	public boolean onShowInputRequested(int flags, boolean configChange) {
		InputConnection ic = getCurrentInputConnection();
		// put mushroom's result string
		if (mReceiveString != null && ic != null) {
			commitTextSKK(ic, mReceiveString, 1);
			mReceiveString = null;
		}  
		return super.onShowInputRequested(flags, configChange);
	}
}
