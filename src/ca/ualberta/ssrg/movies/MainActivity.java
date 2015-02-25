package ca.ualberta.ssrg.movies;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity.Header;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import ca.ualberta.ssrg.androidelasticsearch.R;
import ca.ualberta.ssrg.movies.es.ESMovieManager;
import ca.ualberta.ssrg.movies.es.IMovieManager;
import ca.ualberta.ssrg.movies.es.Movie;
import ca.ualberta.ssrg.movies.es.data.SearchResponse;
import ca.ualberta.ssrg.movies.es.data.SimpleSearchCommand;

public class MainActivity extends Activity {

	private ListView movieList;
	private List<Movie> movies;
	private ArrayAdapter<Movie> moviesViewAdapter;

	private IMovieManager movieManager;

	private Context mContext = this;

	// Thread to update adapter after an operation
	private Runnable doUpdateGUIList = new Runnable() {
		public void run() {
			moviesViewAdapter.notifyDataSetChanged();
		}
		//runOnUIThread(doUpdateGUIList);
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		movieList = (ListView) findViewById(R.id.movieList);
	}

	@Override
	protected void onStart() {
		super.onStart();

		movies = new ArrayList<Movie>();
		moviesViewAdapter = new ArrayAdapter<Movie>(this, R.layout.list_item,movies);
		movieList.setAdapter(moviesViewAdapter);
		movieManager = new ESMovieManager();

		// Show details when click on a movie
		movieList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,	long id) {
				int movieId = movies.get(pos).getId();
				startDetailsActivity(movieId);
			}

		});

		// Delete movie on long click
		movieList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Movie movie = movies.get(position);
				Toast.makeText(mContext, "Deleting " + movie.getTitle(), Toast.LENGTH_LONG).show();

				Thread thread = new DeleteThread(movie.getId());
				// run thread
				thread.start();

				return true;
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// make new thread and start it
		Thread thread = new SearchThread();
		thread.start();
		//
		
		// Refresh the list when visible
		// TODO: Search all
		
	}

	/** 
	 * Search for movies with a given word(s) in the text view
	 * @param view
	 */
	public void search(View view) {
		movies.clear();

		// TODO: Extract search query from text view
		
		// TODO: Run the search thread
		
	}
	
	/**
	 * Starts activity with details for a movie
	 * @param movieId Movie id
	 */
	public void startDetailsActivity(int movieId) {
		Intent intent = new Intent(mContext, DetailsActivity.class);
		intent.putExtra(DetailsActivity.MOVIE_ID, movieId);

		startActivity(intent);
	}
	
	/**
	 * Starts activity to add a new movie
	 * @param view
	 */
	public void add(View view) {
		Intent intent = new Intent(mContext, AddActivity.class);
		startActivity(intent);
	}


	class SearchThread extends Thread {
		// TODO: Implement search thread - done.
		@Override
		public void run() {
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://cmput301.softwareprocess.es.:8080/testing/movie/_search");
			Gson gson = new Gson();
			String string = gson.toJson(new SimpleSearchCommand(""));
			
			StringEntity stringEntity = null;
			try {
				stringEntity = new StringEntity(string);
			} catch (UnsupportedEncodingException e){
				throw new RuntimeException(e);
			}
			
			post.setHeader("Accept", "application/json");
			post.setEntity(stringEntity);
			// try and execute the new document
			
			HttpResponse response;
			
			try {
				client.execute(post);
			} catch (ClientProtocolException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			Type searchResponseType = new TypeToken<SearchResponse<Movie>>() {
			}.getType();
			
			try {
				SearchResponse<Movie> result = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), searchResponseType);
			} catch (JsonIOException e) {
				throw new RuntimeException(e);
			} catch (JsonSyntaxException e) {
				throw new RuntimeException(e);
			} catch (IllegalStateException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	
	class DeleteThread extends Thread {
		private int movieId;

		public DeleteThread(int movieId) {
			this.movieId = movieId;
		}

		@Override
		public void run() {
			// should be called movieController
			movieManager.deleteMovie(movieId);

			// Remove movie from local list
			for (int i = 0; i < movies.size(); i++) {
				Movie m = movies.get(i);

				if (m.getId() == movieId) {
					movies.remove(m);
					break;
				}
			}

			runOnUiThread(doUpdateGUIList);
		}
	}
}