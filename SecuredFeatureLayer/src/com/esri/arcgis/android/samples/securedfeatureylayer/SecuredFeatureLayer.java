/* Copyright 2012 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the Sample code usage restrictions document for further information.
 *
 */

package com.esri.arcgis.android.samples.securedfeatureylayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer.MODE;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.EsriSecurityException;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.ags.query.QueryTask;

public class SecuredFeatureLayer extends Activity {
	// ArcGIS elements
	MapView mMapView;
	ArcGISTiledMapServiceLayer basemap;
	ArcGISFeatureLayer secureFeatureLayer;
	ArcGISDynamicMapServiceLayer dynamicLayer;

	String featureServiceURL;
	Query queryParameter;
	FeatureSet featureSet;

	String response;
	// UI elements
	static ProgressDialog dialog;
	static AlertDialog.Builder alertDialogBuilder;
	static AlertDialog alertDialog;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Retrieve the map and initial extent from XML layout
		mMapView = (MapView) findViewById(R.id.map);
		// create basemap layer
		basemap = new ArcGISTiledMapServiceLayer(this.getResources().getString(
				R.string.basemap_url));
		// add basemap layer to map
		mMapView.addLayer(basemap);

		// create user credentials to access secure services
		UserCredentials creds = new UserCredentials();
		// get user and password from string resource
		final String user = this.getResources().getString(R.string.config_user_name);
		final String pwd = this.getResources().getString(R.string.config_user_passwd);
		// create the user account to access service
		creds.setUserAccount(user, pwd);
		// create the secure service URL
		featureServiceURL = this.getResources().getString(
				R.string.featureserver_url);
		// create the secured layer with credentials
		secureFeatureLayer = new ArcGISFeatureLayer(featureServiceURL,
				MODE.ONDEMAND, creds);
		// add secure layer to the map
		mMapView.addLayer(secureFeatureLayer);
		// enable map to wrap around date line
		mMapView.enableWrapAround(true);
		// add esri attribution to map
		mMapView.setEsriLogoVisible(true);

		// Tap on the map and query the secured feature service
		mMapView.setOnSingleTapListener(new OnSingleTapListener() {
			private static final long serialVersionUID = 1L;

			public void onSingleTap(final float x, final float y) {
				// Create query for the secured feature service
				queryParameter = new Query();
				queryParameter.setInSpatialReference(SpatialReference
						.create(mMapView.getSpatialReference().getID()));
				SpatialReference sr = SpatialReference.create(mMapView
						.getSpatialReference().getID());
				queryParameter.setOutSpatialReference(sr);
				queryParameter.setReturnGeometry(true);
				// set the query condition
				queryParameter.setWhere("confirmed=1");
				// set the response to all
				queryParameter.setOutFields(new String[] { "*" });
				queryParameter.setGeometry(mMapView.getExtent());

				// create a query feature service and execute off the UI thread
				QueryFeatureService queryFS = new QueryFeatureService();
				queryFS.execute(featureServiceURL);
			}
		});

		// Handle status change event on MapView
		mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
			private static final long serialVersionUID = 1L;

			public void onStatusChanged(Object source, STATUS status) {
				if (status == STATUS.LAYER_LOADING_FAILED) {
					// Check if a layer is failed to be loaded due to security
					if ((status.getError()) instanceof EsriSecurityException) {
						EsriSecurityException securityEx = (EsriSecurityException) status
								.getError();
						if (securityEx.getCode() == EsriSecurityException.AUTHENTICATION_FAILED)
							Toast.makeText(mMapView.getContext(),
									"Authentication Failed! Resubmit!",
									Toast.LENGTH_SHORT).show();
						else if (securityEx.getCode() == EsriSecurityException.TOKEN_INVALID)
							Toast.makeText(mMapView.getContext(),
									"Invalid Token! Resubmit!",
									Toast.LENGTH_SHORT).show();
						else if (securityEx.getCode() == EsriSecurityException.TOKEN_SERVICE_NOT_FOUND)
							Toast.makeText(mMapView.getContext(),
									"Token Service Not Found! Resubmit!",
									Toast.LENGTH_SHORT).show();
						else if (securityEx.getCode() == EsriSecurityException.UNTRUSTED_SERVER_CERTIFICATE)
							Toast.makeText(mMapView.getContext(),
									"Untrusted Host! Resubmit!",
									Toast.LENGTH_SHORT).show();

						if (source instanceof ArcGISFeatureLayer) {
							// Set user credential through username and password
							UserCredentials creds = new UserCredentials();
							creds.setUserAccount(user, pwd);
							secureFeatureLayer.reinitializeLayer(creds);
						}
					}
				}
			}
		});

	}

	protected void onPause() {
		super.onPause();
		mMapView.pause();
	}

	protected void onResume() {
		super.onResume();
		mMapView.unpause();
	}

	/**
	 * 
	 * AsyncTask to run query off UI thread
	 * 
	 */
	class QueryFeatureService extends AsyncTask<String, Void, FeatureSet> {

		protected void onPreExecute() {
			dialog = ProgressDialog.show(SecuredFeatureLayer.this, "",
					"Querying Confirmed Birds ...");
		}

		@Override
		protected FeatureSet doInBackground(String... params) {
			String fsURL = params[0];
			// Display query result
			try {
				// Set user credential through username and password
				UserCredentials creds = new UserCredentials();
				creds.setUserAccount(
						SecuredFeatureLayer.this.getResources().getString(
								R.string.config_user_name),
						SecuredFeatureLayer.this.getResources().getString(
								R.string.config_user_passwd));
				// set the secured query service and credentials
				QueryTask query = new QueryTask(fsURL, creds);
				// execute the query
				featureSet = query.execute(queryParameter);
				// return featureset results
				return featureSet;

			} catch (EsriSecurityException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute(FeatureSet featureSet) {
			dialog.dismiss();
			response = "No Birds were found.";

			if (featureSet != null) {
				Graphic[] feature = featureSet.getGraphics();
				if (feature != null && feature.length > 0) {
					response = "A total of " + feature.length
							+ " confirmed birds were found.";
				}
			}
			// send response to user
			alertDialogBuilder = new AlertDialog.Builder(
					SecuredFeatureLayer.this);
			alertDialogBuilder.setTitle("Query Response");
			alertDialogBuilder.setMessage(response);
			alertDialogBuilder.setCancelable(true);
			// create alert dialog
			alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
	}

}