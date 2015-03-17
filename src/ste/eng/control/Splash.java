package ste.eng.control;


import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class Splash extends ListActivity{
	Button buButtonControl, buGestureControl;
	String classNames[] = {"Gesture_control","Gesture_control_Video"};
	String namesToShow[] = {"GoBot","ViBot"};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		
		setListAdapter(new ArrayAdapter<String>(Splash.this,android.R.layout.simple_list_item_1,
				namesToShow));
		
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);

		String temps = classNames[position];
		try{
			Class myclass;
			myclass = Class.forName("ste.eng.control." + temps);
			Intent myintent = new Intent(Splash.this,myclass);
			startActivity(myintent);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	


}
