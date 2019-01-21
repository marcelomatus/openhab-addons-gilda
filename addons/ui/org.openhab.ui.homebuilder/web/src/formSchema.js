import * as _ from 'lodash'
import * as s from 'underscore.string'
import {validators} from 'vue-form-generator'
import {floors, languages, objects, OBJECTS_SUFFIX, rooms} from './definitions'

/**
 * Invoked when language select has changed its value.
 *
 * @param {Object} model
 * @param {Object} newVal
 */
function languageChanged(model, newVal) {
    this.$parent.$parent.$parent.fetchTranslations(newVal);
}

/**
 * Creates a custom room entry
 *
 * @param {string} newTag
 * @param {string} id
 * @param {Object} options
 * @param {string} value
 */
function newRoomTag(newTag, id, options, value) {
    const tag = {
        name: newTag,
        icon: 'none',
        custom: true,
        value: s(newTag)
            .trim()
            .toLowerCase()
            .cleanDiacritics()
            .classify()
            .value()
    };
    rooms.push(tag);
    if (value) {
        value.push(tag);
    }
}

/**
 * Returns a group function for a given
 * Item's type
 * @param {*} type
 */
function getGroupFunc(type) {
    let func = '';

    switch (type) {
        case 'Switch':
            func = 'OR(ON, OFF)';
            break;
        case 'Contact':
            func = 'OR(OPEN, CLOSED)';
            break;
        case 'Rollershutter':
            func = 'OR(UP, DOWN)';
            break;
        case 'Number':
            func = 'AVG';
            break;
        default:
            break;
    }

    return func;
}

/**
 * Creates a custom object entry
 *
 * @param {string} newTag
 * @param {string} id
 * @param {Object} options
 * @param {string} value
 */
function newObjectTag(newTag, id, options, value) {
    let split = newTag.split(':');
    let type = split.length > 1 ? _.first(split).trim() : 'Switch';
    let name = split.length > 1 ? split[1].trim() : newTag;
    let groupFunc = getGroupFunc(type);

    const tag = {
        name: name,
        icon: 'none',
        type: type + ':' + groupFunc,
        unit: '[(%d)]',
        custom: true,
        value: s(name)
            .trim()
            .toLowerCase()
            .cleanDiacritics()
            .classify()
            .value()
    };

    objects.push(tag);
    if (value) {
        value.push(tag);
    }
}

/**
 * Creates a custom floor entry
 *
 * @param {string} newTag
 * @param {string} id
 * @param {Object} options
 * @param {string} value
 */
function newFloorTag(newTag, id, options, value) {
    const tag = {
        abbr: s(newTag)
            .trim()
            .toUpperCase()
            .cleanDiacritics()
            .substr(0, 1)
            .value(),
        name: newTag,
        icon: 'none',
        custom: true,
        value: s(newTag)
            .trim()
            .toLowerCase()
            .cleanDiacritics()
            .classify()
            .value()
    };
    floors.push(tag);
    if (value) {
        value.push(tag);
    }
}

/**
 * Is being executed when
 * collection of rooms in floor vueMultiSelect field
 * has changed.
 *
 * If there's a new `room` in collection,
 * a new dynamic field is added to the floor object, e.g.
 * `"GroundFloor": [ { name: 'Bedroom', value: 'Bedroom', icon: 'bed' }]`
 *
 * If an entry was removed from the collection,
 * a dynamic field is removed accordingly.
 *
 * @param {Object} model
 * @param {Array} newVal
 * @param {Array} oldVal
 * @param {Object} field
 */
function roomsChanged(model, newVal, oldVal, field) {
    let oldList = oldVal ? _.map(oldVal, 'value') : [];
    let newList = _.map(newVal, 'value');
    let lastRemoved = _.first(_.difference(oldList, newList));
    let floor = field.model;

    if (lastRemoved) {
        let roomName = floor + '_' + lastRemoved + OBJECTS_SUFFIX;
        delete model[roomName];
    }
}

/**
 * Schema describing the basic form
 * generated by vue-form-generator
 */
export var basicFields = [
    {
        type: 'select',
        model: 'language',
        label: 'Please select your language',
        values: function () {
            return languages;
        },
        selectOptions: {
            hideNoneSelectedText: true
        },
        onChanged: languageChanged
    },
    {
        type: 'input',
        inputType: 'text',
        label: 'Home Setup Name',
        model: 'homeName',
        readonly: false,
        disabled: false,
        placeholder: 'Home name',
        validator: validators.string
    }
];

export var floorsFields = [{
    type: 'multiselect',
    label: 'Floors',
    styleClasses: 'rooms-list',
    model: 'floors',
    values: floors,
    placeholder: 'Type to search or add floor',
    selectOptions: {
        multiple: true,
        hideSelected: true,
        closeOnSelect: false,
        selectLabel: '',
        trackBy: 'value',
        label: 'name',
        searchable: true,
        taggable: true,
        tagPlaceholder: 'Add this as a new floor',
        onNewTag: newFloorTag
    },
    onChanged: function (model, newVal, oldVal, field) {
        let oldList = oldVal ? _.map(oldVal, 'value') : [];
        let newList = _.map(newVal, 'value');
        let lastRemoved = _.first(_.difference(oldList, newList));

        if (lastRemoved) {
            delete model[lastRemoved];
            for (let property in model) {
                if (model.hasOwnProperty(property) && property.startsWith(lastRemoved + '_')) {
                    delete model[property];
                }
            }
        }
    }
}];

export var settingsFields = [
    {
        type: 'checklist',
        model: 'filesGenerated',
        label: 'What would you like to generate?',
        multi: true,
        listBox: true,
        multiSelect: true,
        values: [
            {name: 'Items', value: 'items'},
            {name: 'Sitemap', value: 'sitemap'},
            {name: 'Dashboard', value: 'habpanel'}
        ]
    },

    {
        type: 'radios',
        label: 'How would you like to store the Items?',
        model: 'itemsType',
        visible(model) {
            return model && model.filesGenerated.includes('items');
        },
        values: [
            {name: 'Textual Configuration Files', value: 'text'},
            {name: 'Internal Database', value: 'rest'}
        ]
    },

    {
        type: 'switch',
        label: 'Append channel to the non-Group items',
        model: 'itemsChannel',
        default: true,
        textOn: 'Yes',
        textOff: 'No',
        valueOn: true,
        valueOff: false,
        visible(model) {
            return model &&
                model.filesGenerated.includes('items') &&
                model.itemsType === 'text';
        }
    },

    {
        type: 'switch',
        label: 'Include icons',
        model: 'itemsIcons',
        default: true,
        textOn: 'Yes',
        textOff: 'No',
        valueOn: true,
        valueOff: false,
        visible(model) {
            return model &&
                (model.filesGenerated.includes('items') || model.filesGenerated.includes('sitemap'));
        }
    },

    {
        type: 'switch',
        label: 'Include tags',
        model: 'itemsTags',
        default: true,
        textOn: 'Yes',
        textOff: 'No',
        valueOn: true,
        valueOff: false,
        visible(model) {
            return model && model.filesGenerated.includes('items');
        }
    }
];

/**
 * Generates a vueMultiSelect input with rooms for specific floor (model)
 *
 * @param {string} model
 * @param {string} label
 */
export function roomsSelect(model, label) {
    return {
        type: 'multiselect',
        label: label,
        styleClasses: 'rooms-list',
        model: model,
        values: rooms,
        placeholder: 'Type to search or add room',
        selectOptions: {
            multiple: true,
            trackBy: 'value',
            label: 'name',
            selectLabel: '',
            searchable: true,
            taggable: true,
            closeOnSelect: false,
            hideSelected: true,
            tagPlaceholder: 'Add this as a new room',
            onNewTag: newRoomTag
        },
        onChanged: roomsChanged
    };
}

/**
 * Generates a vueMultiSelect input with objects for specific rooms (model)
 *
 * @param {string} model
 * @param {string} label
 */
export function objectSelect(model, label) {
    return {
        type: 'multiselect',
        label: label,
        styleClasses: 'rooms-list',
        model: model,
        values: objects,
        placeholder: 'Type to search or add object',
        selectOptions: {
            multiple: true,
            hideSelected: true,
            closeOnSelect: false,
            selectLabel: '',
            trackBy: 'value',
            label: 'name',
            searchable: true,
            taggable: true,
            tagPlaceholder: 'Add this as a new object',
            onNewTag: newObjectTag
        }
    };
}
