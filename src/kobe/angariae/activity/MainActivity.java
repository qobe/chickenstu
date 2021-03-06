package kobe.angariae.activity;

import java.util.ArrayList;

import kobe.angariae.DatabaseHelper;
import kobe.angariae.R;
import kobe.angariae.TrackAdapter;
import kobe.angariae.connection.Connection;
import kobe.angariae.connection.ConnectionAdapter;
import kobe.angariae.connection.FTPConnection;
import kobe.angariae.connection.HTTPConnection;
import kobe.angariae.connection.LocalConnection;
import kobe.angariae.connection.ParcelableConnection;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {
	
	private SQLiteDatabase db;
	private static final int NEW_CONNECTION_ID = 69;
	private static final int EDIT_CONNECTION_ID = 24;
	public static final String CUSTOM_ACTION_EDIT = "kobe.angariae.activity.ACTION_EDIT_CONNECTION";
	public static final String CUSTOM_ACTION_NEW = "kobe.angariae.activity.ACTION_NEW_CONNECTION";
	private ArrayList<Connection> connections;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
	    DatabaseHelper dHelper = new DatabaseHelper(MainActivity.this);
	    db = dHelper.getWritableDatabase();
        
        TextView a = (TextView)findViewById(R.id.new_connection);
        a.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, AddConnectionActivity.class);
				i.setAction(CUSTOM_ACTION_NEW);
				startActivityForResult(i, NEW_CONNECTION_ID);
			}
        });

        connections = new ArrayList<Connection>();
        ConnectionAdapter ca = new ConnectionAdapter(MainActivity.this,R.layout.list_connection, connections);
        setListAdapter(ca);
        registerForContextMenu(getListView());
        
        //Access database and pull connection information
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME,DatabaseHelper.FIELDS, null, null, null, null, null);
        if(cursor != null && cursor.moveToFirst()){
        	Connection c;
        	for(int i=0;i<cursor.getCount();i++){
        		if(cursor.getString(cursor.getColumnIndex(DatabaseHelper.TYPE)).contains("FTP")){
        			c = new FTPConnection();
        		}else{
        			c = new HTTPConnection();
        		}
        		c.setLabel(cursor.getString(cursor.getColumnIndex(DatabaseHelper.LABEL)));
        		c.setServerAddress(cursor.getString(cursor.getColumnIndex(DatabaseHelper.SERVER_ADDRESS)));
        		c.setUserName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.USER_NAME)));
        		c.setPassword(cursor.getString(cursor.getColumnIndex(DatabaseHelper.PASSWORD)));
        		connections.add(c);
        		cursor.moveToNext();
        	}
        }
        //local music only
        TextView b = (TextView)findViewById(R.id.local_connection);
        b.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		ParcelableConnection pc = new ParcelableConnection(new LocalConnection());
        		Intent i = new Intent(MainActivity.this, BrowseActivity.class);
        		i.putExtra(Connection.klass,pc);
        		startActivity(i);
        	}
        });
      }
    
	@Override
	protected void onResume(){
		super.onResume();
		((ConnectionAdapter)getListAdapter()).notifyDataSetChanged();
	}
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
		ParcelableConnection pc = new ParcelableConnection(connections.get(position));
		Intent i = new Intent(MainActivity.this, BrowseActivity.class);
		i.putExtra(Connection.klass, pc);
		startActivity(i);//Launch BrowseActivity pass Connection by Intent
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	super.onActivityResult(requestCode, resultCode, data);
    	if(resultCode == RESULT_OK){
    		ParcelableConnection pc = (ParcelableConnection)data.getParcelableExtra(Connection.klass);
    		Connection c = pc.getConnection();
    		connections.add(c);
    		((ConnectionAdapter)getListAdapter()).notifyDataSetChanged();
    		switch(requestCode){
    			case NEW_CONNECTION_ID:
    				db.execSQL(String.format("INSERT INTO %s VALUES ('%s','%s','%s','%s','%s');",
    						DatabaseHelper.TABLE_NAME, c.getLabel(), c.getServerAddress(),
    						c.getType(), c.getUserName(), c.getPassword()));
    				break;
    			case EDIT_CONNECTION_ID:
    				db.execSQL(String.format("UPDATE %s SET %s='%s',%s='%s',%s='%s',%s='%s' WHERE %s='%s';",
    						DatabaseHelper.TABLE_NAME, DatabaseHelper.SERVER_ADDRESS, c.getServerAddress(),
    						DatabaseHelper.TYPE, c.getType(), DatabaseHelper.USER_NAME, c.getUserName(),
    						DatabaseHelper.PASSWORD, c.getPassword(), DatabaseHelper.LABEL, c.getLabel()));
    				break;
    			default: break;
    		}
    	}
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
    	super.onCreateContextMenu(menu, v, menuInfo);
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
    	menu.setHeaderTitle(connections.get(info.position).getLabel());
    	menu.add(0, 1, 0, "Edit connection");
    	menu.add(0, 2, 1, "Delete connection");
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	Connection c = connections.get(info.position);
    	switch (item.getItemId()) {
    	case 1:// launch edit connection activity
    		Intent i = new Intent(MainActivity.this, AddConnectionActivity.class);
    		ParcelableConnection pc = new ParcelableConnection(c);
    		i.putExtra(Connection.klass, pc);
			i.setAction(CUSTOM_ACTION_EDIT);
			startActivityForResult(i, EDIT_CONNECTION_ID);
			break;
    	case 2://delete connection: remove from DB
    		db.execSQL(String.format("DELETE FROM %s WHERE %s='%s' AND %s='%s' AND %s='%s';",
    				DatabaseHelper.TABLE_NAME, DatabaseHelper.LABEL, c.getLabel(),
    				DatabaseHelper.SERVER_ADDRESS, c.getServerAddress(),
    				DatabaseHelper.TYPE, c.getType()));
    		connections.remove(info.position);
    		((ConnectionAdapter)getListAdapter()).notifyDataSetChanged();
    		break;
    	}
		return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
