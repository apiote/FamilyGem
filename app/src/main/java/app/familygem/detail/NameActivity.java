package app.familygem.detail;

import static app.familygem.Global.gc;

import android.text.InputType;

import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

import app.familygem.DetailActivity;
import app.familygem.Global;
import app.familygem.Memory;
import app.familygem.ProfileFactsFragment;
import app.familygem.R;
import app.familygem.U;
import app.familygem.util.ChangeUtils;

public class NameActivity extends DetailActivity {

    Name name;

    @Override
    public void format() {
        placeSlug("NAME", null);
        name = (Name)cast(Name.class);
        setTitle(ProfileFactsFragment.writeNameTitle(name));
        int capWords = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        if (Global.settings.expert)
            place(getString(R.string.value), "Value", true, capWords);
        else {
            String givenName = "";
            String surname = "";
            String value = name.getValue();
            if (value != null) {
                givenName = value.replaceAll("/.*?/", "").trim(); // Remove the surname
                if (value.indexOf('/') < value.lastIndexOf('/'))
                    surname = value.substring(value.indexOf('/') + 1, value.lastIndexOf('/')).trim();
            }
            placePiece(getString(R.string.given), givenName, 4043, capWords);
            placePiece(getString(R.string.surname), surname, 6064, capWords);
        }
        place(getString(R.string.nickname), "Nickname", true, capWords);
        place(getString(R.string.type), "Type"); // _TYPE in GEDCOM 5.5, TYPE in GEDCOM 5.5.1
        place(getString(R.string.prefix), "Prefix", Global.settings.expert, capWords);
        place(getString(R.string.given), "Given", Global.settings.expert, capWords);
        place(getString(R.string.surname_prefix), "SurnamePrefix", Global.settings.expert, capWords);
        place(getString(R.string.surname), "Surname", Global.settings.expert, capWords);
        place(getString(R.string.suffix), "Suffix", Global.settings.expert, capWords);
        place(getString(R.string.married_name), "MarriedName", false, capWords); // _marrnm
        place(getString(R.string.aka), "Aka", false, capWords); // _aka
        place(getString(R.string.romanized), "Romn", Global.settings.expert, capWords);
        place(getString(R.string.phonetic), "Fone", Global.settings.expert,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PHONETIC);
        placeExtensions(name);
        U.placeNotes(box, name, true);
        U.placeMedia(box, name, true); // Per GEDCOM 5.5.1 a Name should not contain Media
        U.placeSourceCitations(box, name);
    }

    @Override
    public void delete() {
        Person currentPerson = gc.getPerson(Global.indi);
        currentPerson.getNames().remove(name);
        ChangeUtils.INSTANCE.updateChangeDate(currentPerson);
        Memory.setInstanceAndAllSubsequentToNull(name);
    }
}
