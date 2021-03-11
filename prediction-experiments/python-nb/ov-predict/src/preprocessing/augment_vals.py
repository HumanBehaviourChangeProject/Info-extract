import re
PATTERN = re.compile("(?<![0-9])-?[0-9]*\.?[0-9]+")

def add_value_feature(in_fn, out_fn):
    """Read (text) file of (dense) vectors and add a final value to the vector for the actual value of the node.
    This allows to represent the distance between nodes. Normalize this feature between -1 and 1.
    For numerical -1 is the minimum value in the range, 1 the max.
    For BCT, +1 is the presence, -1 is the absence
    For categorical we create ranges bet. -1 and 1.
    For other, pick some random number close to 0.
    """

    print ("Writing appended vec file at %s" %(out_fn))
    random.seed(123)
    att_values, type_att = collect_attribute_value_maps(in_fn)
    numeric_atts = infer_numerical_attributes(att_values, type_att)

    att_maxes, att_mins = get_att_max_min(att_values, numeric_atts)  # max/min used for normalization

    # debug -- print maxes and mins
    print("There are %d numeric attributes." % len(numeric_atts))
    for num_att_id in numeric_atts:
        print("Numeric att: %s -- Min: %f ; Max: %f" % (num_att_id, att_mins[num_att_id], att_maxes[num_att_id]))

    # go through the file again and add 'normalized' values
    with open(in_fn) as f:
        with open(out_fn, 'w') as f_out:
            for line in f:
                cols = line.split()
                if len(cols) == 2:  # first line
                    f_out.write(line)
                    continue
                prefix, att_id, val = cols[0].split(':', 2)
                # BCTs stay the same
                if prefix == 'I':
                    norm_val = val
                # numerical attributes get normalized
                elif att_id in numeric_atts:
                    match = PATTERN.search(val)
                    if match is not None:
                        num = float(match.group(0))
                        # max-min normalization
                        if att_maxes[att_id] == att_mins[att_id]:
                            norm_val = "1"
                        else:
                            norm_num = 2 * ((num - att_mins[att_id]) / (att_maxes[att_id] - att_mins[att_id])) - 1
                            norm_val = str(norm_num)
                    else:
                        norm_val = "%f" % random.gauss(0, 0.001)
                # TODO not keeping track of categorical attributes yet
                # remaining attributes will get a random value close to zero (not sure what else to do with them)
                else:
                    norm_val = "%f" % random.gauss(0, 0.001)
                # f_out.write(cols[0] + '\t' + norm_val + '\n')
                f_out.write('{0} {1}\n'.format(line.strip(), norm_val))


def get_att_max_min(att_values, numeric_atts):
    # normalize numeric attributes
    att_maxes = {}
    att_mins = {}
    for num_att_id in numeric_atts:
        # get max and min
        nums = []
        for val in att_values[num_att_id]:
            # does val have a number
            match = PATTERN.search(val)
            if match is not None:
                num = float(match.group(0))
                nums.append(num)
        att_mins[num_att_id] = min(nums)
        att_maxes[num_att_id] = max(nums)
    return att_maxes, att_mins


def infer_numerical_attributes(att_values, type_att):
    # check if attribute is numerical (use logic from Martin's Java code)
    numeric_atts = []
    for att_id, vals in att_values.items():
        if att_id in type_att['I']:
            continue  # interventions will all be '1'
        num_val = 0
        for val in vals:
            # does val have a number
            match = PATTERN.search(val)
            if match is not None:
                num_val += 1
        # if 80% or more have numbers, then consider numeric
        if num_val / len(vals) >= 0.8:
            numeric_atts.append(att_id)
    return numeric_atts


def collect_attribute_value_maps(fn):
    att_values = {}
    type_att = {'C': set(), 'I': set(), 'O': set(), 'V': set()}
    with open(fn) as f:
        for line in f:
            cols = line.split()
            if len(cols) == 2:  # first line, skip
                continue
            prefix, att_id, val = cols[0].split(':', 2)
            type_att[prefix].add(att_id)
            if att_id in att_values:
                att_values[att_id].append(val)
            else:
                att_values[att_id] = [val]
    print("There are %d attributes." % len(att_values.keys()))
    print("There are %d interventions." % len(type_att['I']))
    return att_values, type_att


