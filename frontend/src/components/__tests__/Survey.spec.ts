import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Survey from '../Survey.vue'

describe('Survey.vue', () => {
  it('renders welcome screen initially', () => {
    const wrapper = mount(Survey)
    expect(wrapper.text()).toContain('Welcome to CaféRater!')
    expect(wrapper.find('.welcome-screen').exists()).toBe(true)
  })

  it('starts survey when button is clicked', async () => {
    const wrapper = mount(Survey)
    
    // Check initial state
    expect(wrapper.find('.welcome-screen').exists()).toBe(true)
    
    // Click start button
    const startButton = wrapper.find('.primary-btn.pulse')
    await startButton.trigger('click')
    
    // Check if app-container is rendered instead of welcome screen
    expect(wrapper.find('.welcome-screen').exists()).toBe(false)
    expect(wrapper.find('.app-container').exists()).toBe(true)
  })
})
