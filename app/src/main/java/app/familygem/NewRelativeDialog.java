package app.familygem;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.familygem.constant.Choice;
import app.familygem.constant.Extra;
import app.familygem.constant.Relation;

/**
 * Dialog to connect a relative (parent, sibling, partner or child) to a person in expert mode,
 * with dropdown menu to choose in which family to add the relative.
 */
public class NewRelativeDialog extends DialogFragment {

    private Person pivot; // Person we are starting from to create or link a relative
    private Family prefParentFamily; // Parent family to be selected in the spinner
    private Family prefSpouseFamily; // Spouse family to be selected in the spinner
    private boolean newPerson; // Link new person or link existing person
    private Fragment fragment;
    private AlertDialog dialog;
    private Spinner spinner;
    private final List<FamilyItem> options = new ArrayList<>();
    private Relation relation;

    public NewRelativeDialog(Person pivot, Family prefParentFamily, Family prefSpouseFamily, boolean newPerson, Fragment fragment) {
        this.pivot = pivot;
        this.prefParentFamily = prefParentFamily;
        this.prefSpouseFamily = prefSpouseFamily;
        this.newPerson = newPerson;
        this.fragment = fragment;
    }

    // Zero-argument constructor: nececessary to re-instantiate this fragment (e.g. rotating the device screen)
    @Keep // Request to don't remove when minify
    public NewRelativeDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        // Recreate dialog
        if (bundle != null) {
            pivot = Global.gc.getPerson(bundle.getString("idPerno"));
            prefParentFamily = Global.gc.getFamily(bundle.getString("idFamFiglio"));
            prefSpouseFamily = Global.gc.getFamily(bundle.getString("idFamSposo"));
            newPerson = bundle.getBoolean("nuovo");
            fragment = getActivity().getSupportFragmentManager().getFragment(bundle, "frammento");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        //builder.setTitle( nuovo ? R.string.new_relative : R.string.link_person );
        View view = requireActivity().getLayoutInflater().inflate(R.layout.new_relative_dialog, null);
        // Spinner per scegliere la famiglia
        spinner = view.findViewById(R.id.newRelative_families);
        ArrayAdapter<FamilyItem> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        ((View)spinner.getParent()).setVisibility(View.GONE); // inizialmente lo spinner è nascosto

        RadioButton ruolo1 = view.findViewById(R.id.newRelative_parent);
        ruolo1.setOnCheckedChangeListener((r, selected) -> {
            if (selected) populateSpinner(Relation.PARENT);
        });
        RadioButton ruolo2 = view.findViewById(R.id.newRelative_sibling);
        ruolo2.setOnCheckedChangeListener((r, selected) -> {
            if (selected) populateSpinner(Relation.SIBLING);
        });
        RadioButton ruolo3 = view.findViewById(R.id.newRelative_partner);
        ruolo3.setOnCheckedChangeListener((r, selected) -> {
            if (selected) populateSpinner(Relation.PARTNER);
        });
        RadioButton ruolo4 = view.findViewById(R.id.newRelative_child);
        ruolo4.setOnCheckedChangeListener((r, selected) -> {
            if (selected) populateSpinner(Relation.CHILD);
        });

        builder.setView(view).setPositiveButton(android.R.string.ok, (dialog, id) -> {
            // Sets some extras that will be passed to PersonEditorActivity or to PersonsFragment and will arrive to addRelative()
            Intent intent = new Intent();
            intent.putExtra(Extra.PERSON_ID, pivot.getId());
            intent.putExtra(Extra.RELATION, relation);
            FamilyItem familyItem = (FamilyItem)spinner.getSelectedItem();
            if (familyItem.family != null)
                intent.putExtra(Extra.FAMILY_ID, familyItem.family.getId());
            else if (familyItem.parent != null) // We use DESTINATION to convey the parent's ID (the third actor in the scene)
                intent.putExtra(Extra.DESTINATION, "NEW_FAMILY_OF" + familyItem.parent.getId());
            else if (familyItem.existing) // Conveys to PersonsFragment the intention to join an existing family
                intent.putExtra(Extra.DESTINATION, "EXISTING_FAMILY");
            if (newPerson) { // Link new person
                intent.setClass(getContext(), PersonEditorActivity.class);
                startActivity(intent);
            } else { // Link existing person
                intent.putExtra(Choice.PERSON, true);
                intent.setClass(getContext(), Principal.class);
                if (fragment != null)
                    fragment.startActivityForResult(intent, 1401);
                else
                    getActivity().startActivityForResult(intent, 1401);
            }
        }).setNeutralButton(R.string.cancel, null);
        dialog = builder.create();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); // Initially disabled
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("idPerno", pivot.getId());
        if (prefParentFamily != null)
            bundle.putString("idFamFiglio", prefParentFamily.getId());
        if (prefSpouseFamily != null)
            bundle.putString("idFamSposo", prefSpouseFamily.getId());
        bundle.putBoolean("nuovo", newPerson);
        //Save the fragment's instance
        if (fragment != null)
            getActivity().getSupportFragmentManager().putFragment(bundle, "frammento", fragment);
    }

    // Dice se in una famiglia c'è spazio vuoto per aggiungere uno dei due genitori
    boolean carenzaConiugi(Family fam) {
        return fam.getHusbandRefs().size() + fam.getWifeRefs().size() < 2;
    }

    private void populateSpinner(Relation relation) {
        this.relation = relation;
        options.clear();
        int select = -1; // Index of the item to be selected in the spinner
        // If it remains -1, selects the first spinner entry
        switch (relation) {
            case PARENT:
                for (Family fam : pivot.getParentFamilies(Global.gc)) {
                    addOption(new FamilyItem(getContext(), fam));
                    if ((fam.equals(prefParentFamily)   // Seleziona la famiglia preferenziale in cui è figlio
                            || select < 0)           // oppure la prima disponibile
                            && carenzaConiugi(fam)) // se hanno spazio genitoriale vuoto
                        select = options.size() - 1;
                }
                addOption(new FamilyItem(getContext(), false));
                if (select < 0)
                    select = options.size() - 1; // Seleziona "Nuova famiglia"
                break;
            case SIBLING:
                for (Family fam : pivot.getParentFamilies(Global.gc)) {
                    addOption(new FamilyItem(getContext(), fam));
                    for (Person padre : fam.getHusbands(Global.gc)) {
                        for (Family fam2 : padre.getSpouseFamilies(Global.gc))
                            if (!fam2.equals(fam))
                                addOption(new FamilyItem(getContext(), fam2));
                        addOption(new FamilyItem(getContext(), padre));
                    }
                    for (Person madre : fam.getWives(Global.gc)) {
                        for (Family fam2 : madre.getSpouseFamilies(Global.gc))
                            if (!fam2.equals(fam))
                                addOption(new FamilyItem(getContext(), fam2));
                        addOption(new FamilyItem(getContext(), madre));
                    }
                }
                addOption(new FamilyItem(getContext(), false));
                // Seleziona la famiglia preferenziale come figlio
                select = 0;
                for (FamilyItem voce : options)
                    if (voce.family != null && voce.family.equals(prefParentFamily)) {
                        select = options.indexOf(voce);
                        break;
                    }
                break;
            case PARTNER:
            case CHILD:
                for (Family fam : pivot.getSpouseFamilies(Global.gc)) {
                    addOption(new FamilyItem(getContext(), fam));
                    if ((options.size() > 1 && fam.equals(prefSpouseFamily)) // Seleziona la famiglia preferita come coniuge (tranne la prima)
                            || (carenzaConiugi(fam) && select < 0)) // Seleziona la prima famiglia dove mancano coniugi
                        select = options.size() - 1;
                }
                addOption(new FamilyItem(getContext(), pivot));
                if (select < 0)
                    select = options.size() - 1; // Seleziona "Nuova famiglia di..."
                // For a child, selects the preferred family (if any), otherwise the first one
                if (relation == Relation.CHILD) {
                    select = 0;
                    for (FamilyItem voce : options)
                        if (voce.family != null && voce.family.equals(prefSpouseFamily)) {
                            select = options.indexOf(voce);
                            break;
                        }
                }
        }
        if (!newPerson) {
            addOption(new FamilyItem(getContext(), true));
        }
        ArrayAdapter<FamilyItem> adapter = (ArrayAdapter)spinner.getAdapter();
        adapter.clear();
        adapter.addAll(options);
        ((View)spinner.getParent()).setVisibility(View.VISIBLE);
        spinner.setSelection(select);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }

    private void addOption(FamilyItem item) {
        if (!options.contains(item)) options.add(item);
    }

    /**
     * Container of a family entry for the list of "Inside" spinner.
     */
    static class FamilyItem {
        Context context;
        Family family;
        Person parent;
        boolean existing; // Pivot will try to join the already existing family

        // Existing family
        FamilyItem(Context context, Family family) {
            this.context = context;
            this.family = family;
        }

        // New family of a parent
        FamilyItem(Context context, Person parent) {
            this.context = context;
            this.parent = parent;
        }

        // New empty family (false) or family that will be acquired from a recipient (true)
        FamilyItem(Context context, boolean existing) {
            this.context = context;
            this.existing = existing;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FamilyItem) {
                FamilyItem that = (FamilyItem)obj;
                return existing == that.existing && Objects.equals(family, that.family) && Objects.equals(parent, that.parent);
            }
            return false;
        }

        @NonNull
        @Override
        public String toString() {
            if (family != null)
                return U.testoFamiglia(context, Global.gc, family, true);
            else if (parent != null)
                return context.getString(R.string.new_family_of, U.properName(parent));
            else if (existing)
                return context.getString(R.string.existing_family);
            else
                return context.getString(R.string.new_family);
        }
    }
}
