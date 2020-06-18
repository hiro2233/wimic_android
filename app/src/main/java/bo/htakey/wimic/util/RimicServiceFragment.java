/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bo.htakey.wimic.util;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import bo.htakey.rimic.IRimicService;
import bo.htakey.rimic.util.IRimicObserver;
import bo.htakey.wimic.service.IWimicService;

/**
 * Fragment class intended to make binding the Rimic service to fragments easier.
 * Created by andrew on 04/08/13.
 */
public abstract class RimicServiceFragment extends Fragment {

    private RimicServiceProvider mServiceProvider;

    /** State boolean to make sure we don't double initialize a fragment once a service has been bound. */
    private boolean mBound;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mServiceProvider = (RimicServiceProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement RimicServiceProvider");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mServiceProvider.addServiceFragment(this);
        if(mServiceProvider.getService() != null && !mBound)
            onServiceAttached(mServiceProvider.getService());
    }

    @Override
    public void onDestroy() {
        mServiceProvider.removeServiceFragment(this);
        if(mServiceProvider.getService() != null && mBound)
            onServiceDetached(mServiceProvider.getService());
        super.onDestroy();
    }

    /** The definitive place where data from the service will be used to initialize the fragment. Only called once per bind, whether the fragment loads first or the service. */
    public void onServiceBound(IRimicService service) { }

    public void onServiceUnbound() { }

    /** If implemented, will register the returned observer to the service upon binding. */
    public IRimicObserver getServiceObserver() {
        return null;
    }

    private void onServiceAttached(IRimicService service) {
        mBound = true;
        if(getServiceObserver() != null)
            service.registerObserver(getServiceObserver());

        onServiceBound(service);
    }

    private void onServiceDetached(IRimicService service) {
        mBound = false;
        if(getServiceObserver() != null)
            service.unregisterObserver(getServiceObserver());

        onServiceUnbound();
    }

    public void setServiceBound(boolean bound) {
        if(bound && !mBound)
            onServiceAttached(mServiceProvider.getService());
        else if(mBound && !bound)
            onServiceDetached(mServiceProvider.getService());
    }

    public IWimicService getService() {
        return mServiceProvider.getService();
    }
}
