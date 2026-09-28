package local.aicenter.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Deterministic grading for offline-model acceptance. The model never grades itself. */
public final class AcceptanceEvaluator {
    public enum Kind { CONTAINS_ALL, REGEX, EXACT, NUMERIC, MANUAL_REVIEW }
    public enum Status { PASS, FAIL, MANUAL_REVIEW }

    public static final class Rule {
        public final Kind kind;
        public final List<String> required;
        public final List<String> forbidden;
        public final String pattern;
        public final String expected;
        public final double tolerance;

        public Rule(Kind kind,List<String> required,List<String> forbidden,String pattern,String expected,double tolerance){
            this.kind=kind;
            this.required=immutable(required);
            this.forbidden=immutable(forbidden);
            this.pattern=pattern==null?"":pattern;
            this.expected=expected==null?"":expected;
            this.tolerance=tolerance;
            if(kind==null)throw new IllegalArgumentException("Missing rule kind");
            if(tolerance<0||!Double.isFinite(tolerance))throw new IllegalArgumentException("Invalid tolerance");
        }
        private static List<String> immutable(List<String> input){
            if(input==null)return Collections.emptyList();
            ArrayList<String> copy=new ArrayList<>();
            for(String item:input){if(item==null||item.trim().isEmpty())throw new IllegalArgumentException("Empty criterion");copy.add(item);}
            return Collections.unmodifiableList(copy);
        }
    }

    public static final class Result {
        public final Status status;
        public final String reason;
        public Result(Status status,String reason){this.status=status;this.reason=reason;}
    }

    private AcceptanceEvaluator(){}

    public static Result evaluate(String answer,Rule rule){
        if(answer==null||answer.trim().isEmpty())return new Result(Status.FAIL,"empty_answer");
        if(rule.kind==Kind.MANUAL_REVIEW)return new Result(Status.MANUAL_REVIEW,"manual_review_required");
        String semantic=normalize(answer);
        for(String denied:rule.forbidden){
            if(semantic.contains(normalize(denied)))return new Result(Status.FAIL,"forbidden:"+denied);
        }
        switch(rule.kind){
            case CONTAINS_ALL:
                for(String required:rule.required){
                    if(!semantic.contains(normalize(required)))return new Result(Status.FAIL,"missing:"+required);
                }
                return new Result(Status.PASS,"all_required_terms_present");
            case REGEX:
                if(rule.pattern.isEmpty())return new Result(Status.FAIL,"invalid_empty_pattern");
                return Pattern.compile(rule.pattern,Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.UNICODE_CASE).matcher(answer).matches()
                    ?new Result(Status.PASS,"regex_match"):new Result(Status.FAIL,"regex_mismatch");
            case EXACT:
                return exact(answer).equals(exact(rule.expected))
                    ?new Result(Status.PASS,"exact_match"):new Result(Status.FAIL,"exact_mismatch");
            case NUMERIC:
                try{
                    double actual=firstNumber(answer);double expected=Double.parseDouble(rule.expected);
                    return Math.abs(actual-expected)<=rule.tolerance
                        ?new Result(Status.PASS,"numeric_match"):new Result(Status.FAIL,"numeric_mismatch:"+actual);
                }catch(RuntimeException error){return new Result(Status.FAIL,"numeric_parse_failed");}
            default: throw new IllegalStateException("Unhandled rule kind");
        }
    }

    public static String normalize(String input){
        return compact(input.replace("**","").replace("__","").replace("`","")).toLowerCase(Locale.ROOT);
    }

    /** ASCII numeric token check that is not confused by adjacent Unicode letters. */
    public static boolean containsStandaloneAsciiNumber(String input,String digits){
        if(input==null||digits==null||!digits.matches("[0-9]+"))return false;
        return Pattern.compile("(?<![0-9])"+Pattern.quote(digits)+"(?![0-9])").matcher(input).find();
    }

    private static String compact(String input){return input.replaceAll("\\s+","").trim();}
    private static String exact(String input){return compact(input.replace("`","")).toLowerCase(Locale.ROOT);}

    private static double firstNumber(String input){
        java.util.regex.Matcher matcher=Pattern.compile("[-+]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)").matcher(input.replace(",",""));
        if(!matcher.find())throw new NumberFormatException("No number");
        return new BigDecimal(matcher.group()).doubleValue();
    }
}
