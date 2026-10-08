import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DashboardSelect from '../DashboardSelect.vue'

const options = [
  { value: 'SUBCATEGORIES', label: 'Menu subcategories' },
  { value: 'AGE_GROUPS', label: 'Age groups' },
  { value: 0.05, label: 'α = 0.05 (95% Confidence)' },
]

describe('DashboardSelect', () => {
  it('keeps a labeled combobox operable with arrow keys and Enter', async () => {
    const wrapper = mount(DashboardSelect, {
      props: {
        id: 'anova-factor',
        label: 'Grouping factor',
        modelValue: 'SUBCATEGORIES',
        options,
      },
    })

    const trigger = wrapper.get('[role="combobox"]')
    expect(trigger.attributes('aria-labelledby')).toContain('anova-factor-label')

    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    await trigger.trigger('keydown', { key: 'ArrowDown' })
    await trigger.trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['AGE_GROUPS'])
    expect(trigger.attributes('aria-expanded')).toBe('false')
  })

  it('closes on Escape without changing the selected value', async () => {
    const wrapper = mount(DashboardSelect, {
      props: {
        id: 'anova-alpha',
        label: 'Significance level',
        modelValue: 0.05,
        options,
      },
    })

    const trigger = wrapper.get('[role="combobox"]')
    await trigger.trigger('click')
    await trigger.trigger('keydown', { key: 'Escape' })

    expect(trigger.attributes('aria-expanded')).toBe('false')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })
})
