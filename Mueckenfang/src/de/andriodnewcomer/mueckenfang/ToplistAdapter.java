package de.andriodnewcomer.mueckenfang;
import java.util.List;

import android.content.Context;
import android.text.TextUtils.SimpleStringSplitter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
public class ToplistAdapter extends ArrayAdapter<String>{

	private Context context;
	private List<String> toplist;

	public ToplistAdapter(Context ctx, int textViewResourceId,
			List<String> list) {
		super(ctx, textViewResourceId, list);
		context = ctx;
		toplist = list;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View element = convertView;
		if (element==null){
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			element = inflater.inflate(R.layout.toplist_element, null);
		}
		TextView platz = (TextView) element.findViewById(R.id.platz);
		TextView name = (TextView) element.findViewById(R.id.name);
		TextView punkte = (TextView) element.findViewById(R.id.punkte);
		platz.setText(Integer.toString(position+1)+".");
		SimpleStringSplitter sss = new SimpleStringSplitter(',');
		sss.setString(toplist.get(position));
		name.setText(sss.next());
		punkte.setText(sss.next());
		return element;
	}
	
	@Override
	public int getCount() {
		return toplist.size();
	}
}
