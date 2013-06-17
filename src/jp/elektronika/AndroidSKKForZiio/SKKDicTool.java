package jp.elektronika.AndroidSKKForZiio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class SKKDicTool extends ListActivity {
	private String BTREE_NAME = SKKDictionary.BTREE_NAME;
	private String USER_DICT = SKKDictionary.USER_DICT;
	private RecordManager mRecMan;
	private long mRecID;
	private BTree mBtree;
	private boolean isOpened = false;
	List<Tuple> mEntryList = new ArrayList<Tuple>();
	private int mPositionToRemove = 0;
	private File mExternalStorageDir = null;
	private static final int DIALOG_USERDIC_NOTFOUND = 0;
	private static final int DIALOG_REMOVE_ENTRY = 1;
	private static final int DIALOG_CONFIRM_CLEAR = 2;
	private static final int DIALOG_WRITTEN = 3;
	private static final int REQUEST_TEXTDIC = 0;
	private static final int REQUEST_DIC = 1;
	private String mDictDir = "/mnt/sdcard/";

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dictool);
		mExternalStorageDir = Environment.getExternalStorageDirectory();
		mDictDir = SKKPrefs.getPrefDictDir(getBaseContext());

		findViewById(R.id.ButtonImportTextDic).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				openFileActivity(REQUEST_TEXTDIC);
			}
		});
		findViewById(R.id.ButtonImportDic).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				openFileActivity(REQUEST_DIC);
			}
		});
		findViewById(R.id.ButtonBackup).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				writeToExternalStorage();
				showDialog(DIALOG_WRITTEN);
			}
		});
		findViewById(R.id.ButtonClear).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_CONFIRM_CLEAR);
			}
		});
	}

	private void openFileActivity(int requestCode) {
		Intent intent = new Intent(SKKDicTool.this, FileChooser.class);
		intent.putExtra(FileChooser.KEY_MODE, FileChooser.MODE_OPEN);
//		intent.putExtra(FileChooser.KEY_DIRNAME, mExternalStorageDir.getPath());
		intent.putExtra(FileChooser.KEY_DIRNAME, mDictDir);
		startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		openUserDict();
		String str = null;

		switch (requestCode) {
		case REQUEST_TEXTDIC:
			if (resultCode == RESULT_OK) {
				str = intent.getStringExtra(FileChooser.KEY_FILEPATH);
				if (str == null) return;
				importTextDic(str);
			}
			break;
		case REQUEST_DIC:
			if (resultCode == RESULT_OK) {
				str = intent.getStringExtra(FileChooser.KEY_FILEPATH);
				if (str == null) return;
				importDic(str);
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!isOpened) openUserDict();
	}

	@Override public void onPause() {
		closeUserDict();

		super.onPause();
	}

	private void recreateUserDic() {
		closeUserDict();

		deleteFile(USER_DICT + ".db");
		deleteFile(USER_DICT + ".lg");

		try {
			mRecMan = RecordManagerFactory.createRecordManager(
					SKKUtils.removeSurplusSeparator(mDictDir + File.separator + USER_DICT));
			mBtree = BTree.createInstance(mRecMan, new StringComparator());
			mRecMan.setNamedObject(BTREE_NAME, mBtree.getRecid());
			mRecMan.commit();
			isOpened = true;
			SKKUtils.dlog("New user dictionary created");
		} catch (Exception e) {
			Log.e("SKK", "recreateUserDic() Error: " + e.toString());
		}

		updateListItems();
	}

	private void openUserDict() {
		try {
			mRecMan = RecordManagerFactory.createRecordManager(
					SKKUtils.removeSurplusSeparator(mDictDir + File.separator + USER_DICT));
			mRecID = mRecMan.getNamedObject(BTREE_NAME);

			if (mRecID == 0) {
				showDialog(DIALOG_USERDIC_NOTFOUND);
			} else {
				mBtree = BTree.load(mRecMan, mRecID);
				isOpened = true;
			}
		} catch (Exception e) {
			Log.e("SKKDicTool", "openUserDict() Error: " + e.toString());
		}
		updateListItems();
	}

	private void closeUserDict() {
		if (!isOpened) return;
		try {
			mRecMan.commit();
			mRecMan.close();
			isOpened = false;
		} catch (Exception e) {
			Log.e("SKKDicTool", "closeUserDict() Error: " + e.toString());
		}
		
		Intent retIntent = new Intent(SKKDictDir.ACTION_BROADCAST);
		retIntent.addCategory(SKKDictDir.CATEGORY_BROADCAST);
		retIntent.putExtra(SKKDictDir.VALUE_KEY, "DICT_REOPEN");
		sendBroadcast(retIntent);
	}

	@Override protected void onListItemClick(ListView l, View v, int position, long id) {
		mPositionToRemove = position;
		removeDialog(DIALOG_REMOVE_ENTRY);
		showDialog(DIALOG_REMOVE_ENTRY);
	}

	private void updateListItems() {
		if (!isOpened) {mEntryList.clear(); updateListView(); return;}

		Tuple tuple = new Tuple();
		TupleBrowser browser;

		mEntryList.clear();
		try {
			browser = mBtree.browse();

			while (browser.getNext(tuple) == true) {
				mEntryList.add(new Tuple((String)tuple.getKey(), (String)tuple.getValue()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("SKKDicTool", "updateListItems() Error: " + e.toString());
		}

		updateListView();
	}

	private void updateListView() {
		List<String> displayList = new ArrayList<String>();

		for (Tuple tuple: mEntryList) {
			displayList.add(tuple.getKey() + " " + tuple.getValue());
		}

		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.dictool_listitem, displayList);
		setListAdapter(listAdapter);
	}

	private void addToDic(String key, String value) {
		try {
			String[] val_array = null;
			String[] oldval_array = null;
			String oldval = (String)mBtree.find(key);

			if (oldval != null) {
				val_array = value.split("/");
				oldval_array = oldval.split("/");
				List<String> tmplist = new ArrayList<String>();
				for (int i=1; i<val_array.length; i++) {
					tmplist.add(val_array[i]);
				}
				for (int i=1; i<oldval_array.length; i++) {
					if (tmplist.indexOf(oldval_array[i]) == -1) {
						tmplist.add(oldval_array[i]);
					}
				}

				StringBuilder newValue = new StringBuilder();
				newValue.append("/");
				for (String s: tmplist) {
					newValue.append(s);
					newValue.append("/");
				}
				mBtree.insert(key, newValue.toString(), true);
			} else {
				mBtree.insert(key, value, true);
			}
		} catch (Exception e) {
			Log.e("SKKDicTool", "addToDic() Error: " + e.toString());
		}
	}

	private void importDic(String file) {
		RecordManager tmpRecMan = null;
		long tmpRecID = 0;
		String dbname;
		int point = file.lastIndexOf(".");
		if (point != -1) {
			dbname = file.substring(0, point);
		} else {
			dbname = file;
		}
		
		Log.d("hoge??", dbname);

		try {
			tmpRecMan = RecordManagerFactory.createRecordManager(dbname);
			tmpRecID = tmpRecMan.getNamedObject(BTREE_NAME);
		} catch (Exception e) {
			Log.e("SKKDicTool", "importDic() open tmpRec Error: " + e.toString());
		}
		BTree tmpBtree = null;

		if (tmpRecID == 0) {
			Toast.makeText(SKKDicTool.this, getString(R.string.error_file_not_found, file), Toast.LENGTH_SHORT).show();
			return;
		} else {
			try {
				tmpBtree = BTree.load(tmpRecMan, tmpRecID);
			} catch (Exception e) {
				Log.e("SKKDicTool", "importDic() load btree Error: " + e.toString());
			}
		}

		Tuple tuple = new Tuple();
		TupleBrowser browser;
		try {
			browser = tmpBtree.browse();

			int count=0;
			while (browser.getNext(tuple) == true) {
				addToDic((String)tuple.getKey(), (String)tuple.getValue());
				if (++count % 1000 == 0) {
					mRecMan.commit();
				}
			}
			tmpRecMan.close();
		} catch (Exception e) {
			Log.e("SKKDicTool", "importDic() add entry Error: " + e.toString());
		}

		updateListItems();
	}


	private void importTextDic(String file) {
		FileInputStream fis = null;
		InputStreamReader fr = null;
		try {
			fis = new FileInputStream(file);
			fr = new InputStreamReader(fis, "UTF-8");
		} catch (FileNotFoundException e) {
			Toast.makeText(SKKDicTool.this, getString(R.string.error_file_not_found, file), Toast.LENGTH_SHORT).show();
			return;
		} catch (Exception e) {
			Log.e("SKKDicTool", "importTextDic() open file Error: " + e.toString());
		}
		BufferedReader br = new BufferedReader(fr);

		String line = null;
		String key = null;
		String value = null;
		int idx=0, count=0;

		try {
			for (line = br.readLine(); line != null; line = br.readLine()) {
				if (line.startsWith(";;")) continue;

				idx = line.indexOf(' ');
				if (idx == -1) continue;
				key = line.substring(0, idx);
				value = line.substring(idx + 1, line.length());

				addToDic(key, value);
				if (++count % 1000 == 0) {
					mRecMan.commit();
				}
			}
		} catch (Exception e) {
			Log.e("SKKDicTool", "importTextDic() add entry Error: " + e.toString());
		}

		updateListItems();
	}

	private void writeToExternalStorage() {
		if (!isOpened) return;
		File outputFile = new File(mExternalStorageDir, USER_DICT+".txt");

		Tuple tuple = new Tuple();
		TupleBrowser browser;

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile), 1024);
			browser = mBtree.browse();

			while (browser.getNext(tuple) == true) {
				bw.write((String)tuple.getKey() + " " + (String)tuple.getValue() + "\n");
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			Log.e("SKKDicTool", "writeToExternalStorage() Error: " + e.toString());
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		final EditText editText = new EditText(SKKDicTool.this);
		editText.setText("");

		switch (id) {
		case DIALOG_USERDIC_NOTFOUND:
			dialog = new AlertDialog.Builder(SKKDicTool.this)
				.setMessage(getString(R.string.error_user_dic))
				.setPositiveButton(R.string.label_OK, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						SKKDicTool.this.finish();
					}
				})
				.create();
			break;
		case DIALOG_REMOVE_ENTRY:
			String keyStr = mEntryList.get(mPositionToRemove).getKey().toString();
			dialog = new AlertDialog.Builder(SKKDicTool.this)
				.setMessage(getString(R.string.message_confirm_remove) + "\"" + keyStr + "\"")
				.setPositiveButton(R.string.label_YES, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int b_id) {
						try {
							mBtree.remove(mEntryList.get(mPositionToRemove).getKey());
						} catch (Exception e) {
							Log.e("SKKDicTool", "Error: " + e.toString());
						}
						mEntryList.remove(mPositionToRemove);
						updateListView();
					}
				})
				.setNegativeButton(R.string.label_NO, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int b_id) {
					}
				})
				.setCancelable(true)
				.create();
			break;
		case DIALOG_CONFIRM_CLEAR:
			dialog = new AlertDialog.Builder(SKKDicTool.this)
				.setMessage(getString(R.string.message_confirm_clear))
				.setPositiveButton(R.string.label_YES, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						recreateUserDic();
					}
				})
				.setNegativeButton(R.string.label_NO, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int b_id) {
					}
				})
				.setCancelable(true)
				.create();
			break;
		case DIALOG_WRITTEN:
			dialog = new AlertDialog.Builder(SKKDicTool.this)
				.setMessage(getString(R.string.message_written_to_external_storage, mExternalStorageDir.getPath() + "/" +  USER_DICT + ".txt"))
				.setPositiveButton(R.string.label_OK, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				})
				.create();
			break;
		default:
			dialog = null;
		}

		return dialog;
	}
}
