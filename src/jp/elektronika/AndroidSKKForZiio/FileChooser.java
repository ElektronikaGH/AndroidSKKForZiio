package jp.elektronika.AndroidSKKForZiio;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileChooser extends ListActivity {
	public static final String KEY_DIRNAME = "DirName";
	public static final String KEY_FILENAME = "FileName";
	public static final String KEY_FILEPATH = "FilePath";
	public static final String KEY_FONTSIZE = "FontSize";
	private static final int DEFAULT_FONTSIZE = 16;
	public static final String KEY_MODE = "MODE";
	public static final String MODE_OPEN = "OPEN";
	public static final String MODE_SAVE = "SAVE";

	private String mDirName = null;
	private int mFontSize = DEFAULT_FONTSIZE;
	private String mMode = null;
	private Toast mSearchToast = null;
	private StringBuilder mSearchString = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filechooser);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.getString(KEY_MODE).equals(MODE_SAVE)) {
				setTitle(getString(R.string.label_filechooser_save_as));
				mMode = MODE_SAVE;
			} else {
				setTitle(getString(R.string.label_filechooser_open));
				mMode = MODE_OPEN;
			}

			mDirName = extras.getString(KEY_DIRNAME);
			mFontSize = extras.getInt(KEY_FONTSIZE, DEFAULT_FONTSIZE);
		}

		File dir = new File(mDirName);
		if (mDirName == null || (!dir.isDirectory()) || (!dir.canRead())) {
			mDirName = Environment.getExternalStorageDirectory().getPath();
		}
		fillList();

		findViewById(R.id.ButtonOK).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();

				String fileName = ((EditText)findViewById(R.id.EditTextFileName)).getText().toString();
				if (fileName.length() == 0) {
					setResult(RESULT_CANCELED, intent);
				} else {
					intent.putExtra(KEY_FILENAME, fileName);
					intent.putExtra(KEY_DIRNAME, mDirName);

					String filePath = mDirName + "/" + fileName;
					intent.putExtra(KEY_FILEPATH, filePath);
					setResult(RESULT_OK, intent);
				}
				finish();
			}
		});

		((Button)findViewById(R.id.ButtonCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				setResult(RESULT_CANCELED, intent);
				finish();
			}
		});

		mSearchString = new StringBuilder();
		mSearchToast = Toast.makeText(FileChooser.this, "", Toast.LENGTH_SHORT);
		mSearchToast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 10, 10);
		getListView().setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				ListView lv = (ListView)v;
				boolean gotMatch = false;
				int startIndex = lv.getSelectedItem() == null ? 0 : lv.getSelectedItemPosition();
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if ((event.getDisplayLabel() >= 65 && event.getDisplayLabel() <= 90) || (event.getDisplayLabel() >= 97 && event.getDisplayLabel() <= 122)) {
						char keyChar = Character.toLowerCase(event.getDisplayLabel());
						mSearchString.append(keyChar);
						String str = new String(mSearchString);
						mSearchToast.setText(str);
						mSearchToast.show();
						for (int i=startIndex; i < lv.getCount(); i++) {
							if(((String)lv.getItemAtPosition(i)).toLowerCase().startsWith(str)) {
								gotMatch = true;
								lv.setSelection(i);
								break;
							}
						}
						if (!gotMatch && startIndex > 0) {
							for (int i=0; i < startIndex-1; i++) {
								if(((String)lv.getItemAtPosition(i)).toLowerCase().startsWith(str)) {
									gotMatch = true;
									lv.setSelection(i);
									break;
								}
							}
						}

						return true;
					} else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
						mSearchString.deleteCharAt(mSearchString.length()-1);
						mSearchToast.setText(new String(mSearchString));
						mSearchToast.show();
						return true;
					} else {
						mSearchString = new StringBuilder();
					}
				}

				return false;
			}
		});
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		String item = (String)getListAdapter().getItem(position);
		EditText editText = (EditText)findViewById(R.id.EditTextFileName);

		if (item.equals("..")) {
			if (mDirName.lastIndexOf("/") <= 0) {
				mDirName = mDirName.substring(0, mDirName.lastIndexOf("/") + 1);
			} else {
				mDirName = mDirName.substring(0, mDirName.lastIndexOf("/"));
			}
			fillList();
			editText.setText("");
		} else if (item.substring(item.length() - 1).equals("/")) {
			if (mDirName.equals("/")) {
				mDirName += item;
			} else {
				mDirName = mDirName + "/" + item;
			}
			mDirName = mDirName.substring(0, mDirName.length() - 1);
			fillList();
			editText.setText("");
		} else {
			editText.setText(item);
			if (mMode.equals(MODE_OPEN)) {
				findViewById(R.id.ButtonOK).requestFocus();
			} else {
				editText.requestFocus();
			}
		}
	}

	private void fillList() {
		File[] filesArray = new File(mDirName).listFiles();
		if (filesArray == null) {
			Toast.makeText(FileChooser.this, getString(R.string.error_access_failed, mDirName), Toast.LENGTH_SHORT).show();
			return;
		}
		TextView tv = (TextView)findViewById(R.id.TextDirName);
		tv.setText(mDirName);
		tv.setTextSize(mFontSize+2);

		List<String> dirs = new ArrayList<String>();
		List<String> files = new ArrayList<String>();

		for (File file : filesArray) {
			if (file.isDirectory()) {
				dirs.add(file.getName() + "/");
			} else {
				files.add(file.getName());
			}
		}

		Collections.sort(dirs);
		Collections.sort(files);
		if (!mDirName.equals("/")) {
			dirs.add(0, "..");
		}

		List<String> items = new ArrayList<String>();
		items.addAll(dirs);
		items.addAll(files);
		ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.filechooser_row, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView view = (TextView)super.getView(position, convertView, parent);
				view.setTextSize(mFontSize);
				return view;
			}
		};
		setListAdapter(fileList);
	}
}