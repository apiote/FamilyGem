package app.familygem;

import static app.familygem.Global.gc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.familygem.constant.Extra;
import app.familygem.constant.Gender;
import app.familygem.constant.Relation;
import app.familygem.detail.EventActivity;
import app.familygem.detail.FamilyActivity;
import app.familygem.list.FamiliesFragment;
import app.familygem.util.TreeUtils;

public class PersonEditorActivity extends AppCompatActivity {

    private String personId; // ID of the person we want to edit. If null we have to create a new person.
    private String familyId;
    private Relation relation; // If not null is the relation between the pivot (personId) and the person we have to create
    private Person person; // The person to edit or create
    private RadioButton sexMale;
    private RadioButton sexFemale;
    private RadioButton sexUnknown;
    private int lastChecked;
    private EditText birthDate;
    private DateEditorLayout birthDateEditor;
    private EditText birthPlace;
    private SwitchCompat isDeadSwitch;
    private EditText deathDate;
    private DateEditorLayout deathDateEditor;
    private EditText deathPlace;
    private boolean fromFamilyActivity; // Previous activity was DetailActivity
    private boolean nameFromPieces; // If the given name and surname come from the Given and Surname pieces, they must return there
    private boolean surnameBefore; // The given name comes after the surname, e.g. '/Simpson/ Homer'
    private String nameSuffix; // Last part of the name, not editable here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        U.ensureGlobalGedcomNotNull(gc);
        setContentView(R.layout.edita_individuo);
        Intent intent = getIntent();
        personId = intent.getStringExtra(Extra.PERSON_ID);
        familyId = intent.getStringExtra(Extra.FAMILY_ID);
        relation = (Relation)intent.getSerializableExtra(Extra.RELATION);
        fromFamilyActivity = intent.getBooleanExtra(Extra.FROM_FAMILY, false);
        nameSuffix = "";

        sexMale = findViewById(R.id.sesso1);
        sexFemale = findViewById(R.id.sesso2);
        sexUnknown = findViewById(R.id.sesso3);
        birthDate = findViewById(R.id.data_nascita);
        birthDateEditor = findViewById(R.id.editore_data_nascita);
        birthPlace = findViewById(R.id.luogo_nascita);
        isDeadSwitch = findViewById(R.id.defunto);
        deathDate = findViewById(R.id.data_morte);
        deathDateEditor = findViewById(R.id.editore_data_morte);
        deathPlace = findViewById(R.id.luogo_morte);

        // Toggle sex radio buttons
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        View.OnClickListener radioClick = radioButton -> {
            if (radioButton.getId() == lastChecked) {
                radioGroup.clearCheck();
            }
        };
        sexMale.setOnClickListener(radioClick);
        sexFemale.setOnClickListener(radioClick);
        sexUnknown.setOnClickListener(radioClick);
        radioGroup.setOnCheckedChangeListener((group, checked) -> {
            group.post(() -> {
                lastChecked = checked;
            });
        });

        disableDeath();

        // New person in kinship relationship
        if (relation != null) {
            person = new Person();
            Person pivot = gc.getPerson(personId);
            String surname = null;
            // Sibling's surname
            if (relation == Relation.SIBLING) {
                surname = U.surname(pivot);
            } // Father's surname
            else if (relation == Relation.CHILD) {
                if (fromFamilyActivity) { // Child from FamilyActivity
                    Family fam = gc.getFamily(familyId);
                    if (!fam.getHusbands(gc).isEmpty())
                        surname = U.surname(fam.getHusbands(gc).get(0));
                    else if (!fam.getChildren(gc).isEmpty())
                        surname = U.surname(fam.getChildren(gc).get(0));
                } else { // Child from DiagramFragment or ProfileActivity
                    if (Gender.isMale(pivot))
                        surname = U.surname(pivot);
                    else if (familyId != null) {
                        Family fam = gc.getFamily(familyId);
                        if (fam != null && !fam.getHusbands(gc).isEmpty())
                            surname = U.surname(fam.getHusbands(gc).get(0));
                    }
                }
            }
            ((EditText)findViewById(R.id.cognome)).setText(surname);
        } else if (personId == null) { // New unrelated person
            person = new Person();
        } else { // Gets the data of an existing person to edit them
            person = gc.getPerson(personId);
            // Given name and surname
            if (!person.getNames().isEmpty()) {
                String givenName = "";
                String surname = "";
                Name name = person.getNames().get(0);
                String value = name.getValue();
                if (value != null) {
                    value = value.trim();
                    if (value.indexOf('/') < value.lastIndexOf('/')) { // There is a surname between two "/"
                        if (value.indexOf('/') > 0) givenName = value.substring(0, value.indexOf('/')).trim();
                        surname = value.substring(value.indexOf('/') + 1, value.lastIndexOf('/')).trim();
                        nameSuffix = value.substring(value.lastIndexOf('/') + 1).trim();
                        if (givenName.isEmpty() && !nameSuffix.isEmpty()) { // Given name is after surname
                            givenName = nameSuffix;
                            surnameBefore = true;
                        }
                    } else {
                        givenName = value;
                    }
                } else {
                    if (name.getGiven() != null) {
                        givenName = name.getGiven();
                        nameFromPieces = true;
                    }
                    if (name.getSurname() != null) {
                        surname = name.getSurname();
                        nameFromPieces = true;
                    }
                }
                ((EditText)findViewById(R.id.nome)).setText(givenName);
                ((EditText)findViewById(R.id.cognome)).setText(surname);
            }
            // Sex
            switch (Gender.getGender(person)) {
                case MALE:
                    sexMale.setChecked(true);
                    break;
                case FEMALE:
                    sexFemale.setChecked(true);
                    break;
                case UNKNOWN:
                    sexUnknown.setChecked(true);
            }
            lastChecked = radioGroup.getCheckedRadioButtonId();
            // Birth and death
            for (EventFact fact : person.getEventsFacts()) {
                if (fact.getTag().equals("BIRT")) {
                    if (fact.getDate() != null)
                        birthDate.setText(fact.getDate().trim());
                    if (fact.getPlace() != null)
                        birthPlace.setText(fact.getPlace().trim());
                }
                if (fact.getTag().equals("DEAT")) {
                    isDeadSwitch.setChecked(true);
                    enableDeath();
                    if (fact.getDate() != null)
                        deathDate.setText(fact.getDate().trim());
                    if (fact.getPlace() != null)
                        deathPlace.setText(fact.getPlace().trim());
                }
            }
        }
        birthDateEditor.initialize(birthDate);
        isDeadSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked)
                enableDeath();
            else
                disableDeath();
        });
        deathDateEditor.initialize(deathDate);
        deathPlace.setOnEditorActionListener((vista, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE)
                save();
            return false;
        });

        // Toolbar
        ActionBar toolbar = getSupportActionBar();
        View actionBar = getLayoutInflater().inflate(R.layout.barra_edita, new LinearLayout(getApplicationContext()), false);
        actionBar.findViewById(R.id.edita_annulla).setOnClickListener(v -> onBackPressed());
        actionBar.findViewById(R.id.edita_salva).setOnClickListener(v -> save());
        toolbar.setCustomView(actionBar);
        toolbar.setDisplayShowCustomEnabled(true);
    }

    void disableDeath() {
        findViewById(R.id.morte).setVisibility(View.GONE);
        birthPlace.setImeOptions(EditorInfo.IME_ACTION_DONE);
        birthPlace.setNextFocusForwardId(0);
        // Intercepts the 'Done' on the keyboard
        birthPlace.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE)
                save();
            return false;
        });
    }

    void enableDeath() {
        birthPlace.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        birthPlace.setNextFocusForwardId(R.id.data_morte);
        birthPlace.setOnEditorActionListener(null);
        findViewById(R.id.morte).setVisibility(View.VISIBLE);
    }

    void save() {
        U.ensureGlobalGedcomNotNull(gc); // A crash occurred because gc was null here

        // Name
        String givenName = ((EditText)findViewById(R.id.nome)).getText().toString().trim();
        String surname = ((EditText)findViewById(R.id.cognome)).getText().toString().trim();
        Name name;
        if (person.getNames().isEmpty()) {
            List<Name> names = new ArrayList<>();
            name = new Name();
            names.add(name);
            person.setNames(names);
        } else
            name = person.getNames().get(0);

        if (nameFromPieces) {
            name.setGiven(givenName);
            name.setSurname(surname);
        } else {
            String value = "";
            if (!surname.isEmpty()) value = "/" + surname + "/";
            if (surnameBefore) value += " " + givenName;
            else {
                value = givenName + " " + value;
                if (!nameSuffix.isEmpty()) value += " " + nameSuffix;
            }
            name.setValue(value.trim());
        }

        // Sex
        String chosenGender = null;
        if (sexMale.isChecked())
            chosenGender = "M";
        else if (sexFemale.isChecked())
            chosenGender = "F";
        else if (sexUnknown.isChecked())
            chosenGender = "U";
        if (chosenGender != null) {
            boolean missingSex = true;
            for (EventFact fact : person.getEventsFacts()) {
                if (fact.getTag().equals("SEX")) {
                    fact.setValue(chosenGender);
                    missingSex = false;
                }
            }
            if (missingSex) {
                EventFact sex = new EventFact();
                sex.setTag("SEX");
                sex.setValue(chosenGender);
                person.addEventFact(sex);
            }
            ProfileFactsFragment.updateSpouseRoles(person);
        } else { // Remove existing sex tag
            for (EventFact fact : person.getEventsFacts()) {
                if (fact.getTag().equals("SEX")) {
                    person.getEventsFacts().remove(fact);
                    break;
                }
            }
        }

        // Birth
        birthDateEditor.finishEditing();
        String date = birthDate.getText().toString().trim();
        String place = birthPlace.getText().toString().trim();
        boolean found = false;
        for (EventFact fact : person.getEventsFacts()) {
            if (fact.getTag().equals("BIRT")) {
                /* TODO:
                   if (date.isEmpty() && place.isEmpty() && tagAllEmpty(fact))
                      p.getEventsFacts().remove(fact);
                   More generally, delete a tag when it is empty */
                fact.setDate(date);
                fact.setPlace(place);
                EventActivity.cleanUpTag(fact);
                found = true;
            }
        }
        // If there is any data to save, creates the tag
        if (!found && (!date.isEmpty() || !place.isEmpty())) {
            EventFact birth = new EventFact();
            birth.setTag("BIRT");
            birth.setDate(date);
            birth.setPlace(place);
            EventActivity.cleanUpTag(birth);
            person.addEventFact(birth);
        }

        // Death
        deathDateEditor.finishEditing();
        date = deathDate.getText().toString().trim();
        place = deathPlace.getText().toString().trim();
        found = false;
        for (EventFact fact : person.getEventsFacts()) {
            if (fact.getTag().equals("DEAT")) {
                if (!isDeadSwitch.isChecked()) {
                    person.getEventsFacts().remove(fact);
                } else {
                    fact.setDate(date);
                    fact.setPlace(place);
                    EventActivity.cleanUpTag(fact);
                }
                found = true;
                break;
            }
        }
        if (!found && isDeadSwitch.isChecked()) {
            EventFact death = new EventFact();
            death.setTag("DEAT");
            death.setDate(date);
            death.setPlace(place);
            EventActivity.cleanUpTag(death);
            person.addEventFact(death);
        }

        // Finalization of new person
        Object[] modifications = {person, null}; // The null is used to receive a possible Family
        if (personId == null || relation != null) {
            String newId = U.newID(gc, Person.class);
            person.setId(newId);
            gc.addPerson(person);
            if (Global.settings.getCurrentTree().root == null)
                Global.settings.getCurrentTree().root = newId;
            Global.settings.save();
            if (fromFamilyActivity) { // Comes from FamilyActivity
                Family family = gc.getFamily(familyId);
                FamilyActivity.connect(person, family, relation);
                modifications[1] = family;
            } else if (relation != null) // Comes from DiagramFragment o ProfileRelativesFragment
                modifications = addRelative(personId, newId, familyId, relation, getIntent().getStringExtra(Extra.DESTINATION));
        } else
            Global.indi = person.getId(); // To show the person then in DiagramFragment
        TreeUtils.INSTANCE.save(true, modifications);
        onBackPressed();
    }

    /**
     * Adds a new person in family relation with 'pivot', possibly within the given family.
     *
     * @param familyId Id of the target family. If it is null, a new family is created
     * @param placing  Summarizes how the family was identified and therefore what to do with the people involved
     * @return An array of modified records
     */
    static Object[] addRelative(String pivotId, String newId, String familyId, Relation relation, String placing) {
        Global.indi = pivotId;
        Person newPerson = gc.getPerson(newId);
        // A new family is created in which both pivot and newPerson end up
        if (placing != null && placing.startsWith("NEW_FAMILY_OF")) { // Contains the ID of the parent to create a new family of
            pivotId = placing.substring(13); // The parent actually becomes the pivot
            // Instead of a sibling to pivot, it is as if we were putting a child to the parent
            relation = relation == Relation.SIBLING ? Relation.CHILD : relation;
        }
        // In ListOfPeopleActivity has been identified the family in which will end up the pivot
        else if (placing != null && placing.equals("EXISTING_FAMILY")) {
            newId = null;
            newPerson = null;
        }
        // The new person is welcomed into the pivot family
        else if (familyId != null) {
            pivotId = null; // Pivot is already present in his family and should not be added again
        }
        Family family = familyId != null ? gc.getFamily(familyId) : FamiliesFragment.newFamily(true);
        Person pivot = gc.getPerson(pivotId);
        SpouseRef refSpouse1 = new SpouseRef(), refSposo2 = new SpouseRef();
        ChildRef refChild1 = new ChildRef(), refFiglio2 = new ChildRef();
        ParentFamilyRef parentFamilyRef = new ParentFamilyRef();
        SpouseFamilyRef spouseFamilyRef = new SpouseFamilyRef();
        parentFamilyRef.setRef(family.getId());
        spouseFamilyRef.setRef(family.getId());

        // Population of refs
        switch (relation) {
            case PARENT:
                refSpouse1.setRef(newId);
                refChild1.setRef(pivotId);
                if (newPerson != null) newPerson.addSpouseFamilyRef(spouseFamilyRef);
                if (pivot != null) pivot.addParentFamilyRef(parentFamilyRef);
                break;
            case SIBLING:
                refChild1.setRef(pivotId);
                refFiglio2.setRef(newId);
                if (pivot != null) pivot.addParentFamilyRef(parentFamilyRef);
                if (newPerson != null) newPerson.addParentFamilyRef(parentFamilyRef);
                break;
            case PARTNER:
                refSpouse1.setRef(pivotId);
                refSposo2.setRef(newId);
                if (pivot != null) pivot.addSpouseFamilyRef(spouseFamilyRef);
                if (newPerson != null) newPerson.addSpouseFamilyRef(spouseFamilyRef);
                break;
            case CHILD:
                refSpouse1.setRef(pivotId);
                refChild1.setRef(newId);
                if (pivot != null) pivot.addSpouseFamilyRef(spouseFamilyRef);
                if (newPerson != null) newPerson.addParentFamilyRef(parentFamilyRef);
        }

        if (refSpouse1.getRef() != null)
            addSpouse(family, refSpouse1);
        if (refSposo2.getRef() != null)
            addSpouse(family, refSposo2);
        if (refChild1.getRef() != null)
            family.addChild(refChild1);
        if (refFiglio2.getRef() != null)
            family.addChild(refFiglio2);

        if (relation == Relation.PARENT || relation == Relation.SIBLING) // It will bring up the selected family
            Global.familyNum = gc.getPerson(Global.indi).getParentFamilies(gc).indexOf(family);
        else
            Global.familyNum = 0; // Otherwise resets it

        Set<Object> modified = new HashSet<>();
        if (pivot != null && newPerson != null)
            Collections.addAll(modified, family, pivot, newPerson);
        else if (pivot != null)
            Collections.addAll(modified, family, pivot);
        else if (newPerson != null)
            Collections.addAll(modified, family, newPerson);
        return modified.toArray();
    }

    /**
     * Adds the spouse in a family: always and only on the basis of sex.
     */
    public static void addSpouse(Family family, SpouseRef sr) {
        Person person = Global.gc.getPerson(sr.getRef());
        if (Gender.isFemale(person)) family.addWife(sr);
        else family.addHusband(sr);
    }
}