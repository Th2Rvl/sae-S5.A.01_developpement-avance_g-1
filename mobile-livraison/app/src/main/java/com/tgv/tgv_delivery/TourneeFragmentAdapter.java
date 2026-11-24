package com.tgv.tgv_delivery;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 */
public class TourneeFragmentAdapter extends FragmentStateAdapter {
    /** Nombre de fragments gérés par cet adaptateur */
    private static final int NB_FRAGMENT = 3;
    /**
     * Constructeur de base
     * @param activite activité qui contient le ViewPager2 qui gèrera les fragments
     */
    public TourneeFragmentAdapter(FragmentActivity activite) {
        super(activite);
    }
    @Override
    public Fragment createFragment(int position) {
        switch(position) {
            case 0 :
                return ColisFragment.newInstance();
            case 1 :
                return CarteFragment.newInstance();
            case 2 :
                return BoucleFragment.newInstance();
            default :
                return null;
        }
    }
    @Override
    public int getItemCount() {

        // renvoyer le nombre de fragments gérés par l'adaptateur
        return NB_FRAGMENT;
    }
}